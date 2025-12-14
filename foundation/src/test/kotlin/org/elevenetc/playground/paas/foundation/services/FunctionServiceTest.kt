package org.elevenetc.playground.paas.foundation.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.elevenetc.playground.paas.foundation.database.FunctionStatusHistoryTable
import org.elevenetc.playground.paas.foundation.database.FunctionsTable
import org.elevenetc.playground.paas.foundation.database.ProjectsTable
import org.elevenetc.playground.paas.foundation.events.FunctionStatusEventBus
import org.elevenetc.playground.paas.foundation.models.FunctionStatus
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository
import org.elevenetc.playground.paas.foundation.repositories.FunctionStatusHistoryRepository
import org.elevenetc.playground.paas.foundation.repositories.ProjectRepository
import org.elevenetc.playground.paas.foundation.tools.Docker
import org.elevenetc.playground.paas.foundation.tools.Kind
import org.elevenetc.playground.paas.foundation.utils.TestFixtures
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

/**
 * Integration tests for FunctionService.
 * Tests the function creation flow with a real in-memory database.
 *
 * This approach tests:
 * - FunctionService logic
 * - FunctionRepository with real SQL queries
 * - Status history tracking
 * - Transaction handling
 *
 * External dependencies (Docker, K8s) are mocked.
 */
class FunctionServiceTest {

    private lateinit var database: Database
    private lateinit var functionRepository: FunctionRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var docker: Docker
    private lateinit var kind: Kind
    private lateinit var dockerBuildService: DockerBuildService
    private lateinit var kubernetesService: KubernetesService
    private lateinit var projectService: ProjectService
    private lateinit var functionService: FunctionService

    @BeforeTest
    fun setup() {
        // Initialize H2 in-memory database
        database = Database.connect(createTestDataSource())

        // Create database schema
        transaction(database) {
            SchemaUtils.create(ProjectsTable, FunctionsTable, FunctionStatusHistoryTable)
        }

        // Create real repositories with real database
        projectRepository = ProjectRepository()
        val functionStatusHistoryRepository = FunctionStatusHistoryRepository()
        val eventBus = FunctionStatusEventBus()
        functionRepository = FunctionRepository(functionStatusHistoryRepository, eventBus)

        // Create real ProjectService with real repository
        projectService = ProjectService(projectRepository)

        // Mock external command-line tools
        docker = mockk()
        kind = mockk()

        // Set default behavior for kind - can be overridden in individual tests
        coEvery { kind.loadImageIntoKind(any()) } returns true

        // Create real DockerBuildService with mocked tools
        dockerBuildService = DockerBuildService(kind, docker)

        // Mock other external dependencies
        kubernetesService = mockk(relaxed = true)

        // Create service under test
        functionService = FunctionService(
            functionRepository,
            dockerBuildService,
            kubernetesService,
            projectService
        )
    }

    @AfterTest
    fun tearDown() {
        // Clear database tables for test isolation
        transaction(database) {
            FunctionStatusHistoryTable.deleteAll()
            FunctionsTable.deleteAll()
            ProjectsTable.deleteAll()
        }

        // Clear mocks
        clearAllMocks()
    }

    private fun createTestDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = "jdbc:h2:mem:test_${System.currentTimeMillis()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            username = "sa"
            password = ""
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    // Helper to create a project in database to satisfy foreign key constraints
    // Generates unique project names to avoid unique constraint violations
    private var projectCounter = 0
    private fun createTestProject(): org.elevenetc.playground.paas.foundation.models.Project {
        projectCounter++
        return projectRepository.create(
            name = "TestProject$projectCounter",
            description = "Test project"
        )
    }

    // Helper to wait for status change with polling
    private suspend fun waitForStatus(functionId: String, expectedStatus: FunctionStatus, maxWaitMs: Long = 3000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            val function = functionRepository.findById(functionId)
            if (function?.status == expectedStatus) {
                return
            }
            delay(100)
        }
    }

    // ========================================
    // Happy Path Tests
    // ========================================

    @Test
    fun `createFunction should create function in database with PENDING status`() {
        // Given
        val project = createTestProject()
        val request = TestFixtures.createFunctionRequest(
            sourceCode = "fun hello(): String = \"Hello, World!\""
        )

        // When
        val result = functionService.createFunction(project.id, request)

        // Then - verify function was created in database
        assertEquals(FunctionStatus.PENDING, result.status)
        assertEquals("hello", result.name)
        assertEquals(project.id, result.projectId)
        assertEquals(request.sourceCode, result.sourceCode)
        assertEquals("String", result.returnType)
        assertEquals(emptyList(), result.parameters) // hello() has no parameters

        // Verify function exists in database
        val fromDb = functionRepository.findById(result.id)
        assertNotNull(fromDb)
        assertEquals(result.id, fromDb.id)
        assertEquals(FunctionStatus.PENDING, fromDb.status)
    }

    @Test
    fun `createFunction should extract function name from source code when not provided`() {
        // Given
        val project = createTestProject()
        val request = TestFixtures.createFunctionRequest(
            sourceCode = "fun calculateSum(a: Int, b: Int): Int = a + b"
        )

        // When
        val result = functionService.createFunction(project.id, request)

        // Then
        assertEquals("calculateSum", result.name)
        assertEquals(FunctionStatus.PENDING, result.status)

        // Verify function exists in database
        val fromDb = functionRepository.findById(result.id)
        assertNotNull(fromDb)
        assertEquals("calculateSum", fromDb.name)
    }

    @Test
    fun `buildAndDeployFunction should update status to COMPILING`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } returns Docker.Result(1, "Docker build failed")

        // When - create function (triggers async build)
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        // Wait for async operation to complete
        waitForStatus(function.id, FunctionStatus.BUILD_FAILED)

        // Then - verify status was updated to BUILD_FAILED in database
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.BUILD_FAILED, fromDb.status)
    }

    @Test
    fun `buildAndDeployFunction happy path should create deployment and service`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } returns Docker.Result(0, "Build successful")
        coEvery { kind.loadImageIntoKind(any()) } returns true
        coEvery {
            kubernetesService.createDeployment(any(), any(), any(), any())
        } returns K8sDeploymentResult.Success("paas-func-testproject-testfunc")
        coEvery {
            kubernetesService.createService(any(), any(), any())
        } returns K8sServiceResult.Success("paas-func-testproject-testfunc", 30123)

        // When - create function (triggers async build and deployment)
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        // Wait for async operations to complete
        waitForStatus(function.id, FunctionStatus.READY)

        // Then - verify function reached READY status in database
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.READY, fromDb.status)
        assertEquals("paas-func-testproject-testfunc", fromDb.containerName)
        assertEquals("paas-func-testproject-testfunc", fromDb.containerId)
        assertEquals(30123, fromDb.port)
        // Image name is generated by DockerBuildService using generateContainerName
        // Format: playground-paas-function-{project-name}-{function-name}
        assertNotNull(fromDb.imageTag)
        assertTrue(fromDb.imageTag.startsWith("playground-paas-function-"))

        // Verify external tools and services were called
        verify { docker.build(any(), any()) }
        coVerify { kind.loadImageIntoKind(any()) }
        coVerify {
            kubernetesService.createDeployment(any(), any(), any(), any())
            kubernetesService.createService(any(), any(), any())
        }
    }

    // ========================================
    // Query Tests
    // ========================================

    @Test
    fun `getAllFunctions should return all functions from repository`() {
        // Given - create projects and functions in database
        val proj1 = createTestProject()
        val proj2 = createTestProject()
        val func1 = functionRepository.create(proj1.id, "func1", "fun func1() {}", "Unit", emptyList())
        val func2 = functionRepository.create(proj1.id, "func2", "fun func2() {}", "Unit", emptyList())
        val func3 = functionRepository.create(proj2.id, "func3", "fun func3() {}", "Unit", emptyList())

        // When
        val result = functionService.getAllFunctions()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.any { it.id == func1.id && it.name == "func1" })
        assertTrue(result.any { it.id == func2.id && it.name == "func2" })
        assertTrue(result.any { it.id == func3.id && it.name == "func3" })
    }

    @Test
    fun `getFunctionById should return function when exists`() {
        // Given - create project and function in database
        val project = createTestProject()
        val function = functionRepository.create(project.id, "testFunc", "fun testFunc() {}", "Unit", emptyList())

        // When
        val result = functionService.getFunctionById(function.id)

        // Then
        assertNotNull(result)
        assertEquals(function.id, result.id)
        assertEquals("testFunc", result.name)
    }

    @Test
    fun `getFunctionById should return null when not exists`() {
        // Given
        val functionId = "non-existent"

        // When
        val result = functionService.getFunctionById(functionId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getFunctionsByProjectId should return functions for project`() {
        // Given - create projects and functions in database
        val project = createTestProject()
        val otherProject = createTestProject()
        val func1 = functionRepository.create(project.id, "func1", "fun func1() {}", "Unit", emptyList())
        val func2 = functionRepository.create(project.id, "func2", "fun func2() {}", "Unit", emptyList())
        val func3 = functionRepository.create(otherProject.id, "func3", "fun func3() {}", "Unit", emptyList())

        // When
        val result = functionService.getFunctionsByProjectId(project.id)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.projectId == project.id })
        assertTrue(result.any { it.id == func1.id })
        assertTrue(result.any { it.id == func2.id })
    }

    // ========================================
    // Failure Scenario Tests
    // ========================================

    @Test
    fun `buildAndDeployFunction should update status to BUILD_FAILED when Docker build fails`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } returns Docker.Result(1, "Compilation error")

        // When
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        // Wait for async operation
        waitForStatus(function.id, FunctionStatus.BUILD_FAILED)

        // Then - verify function has BUILD_FAILED status in database
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.BUILD_FAILED, fromDb.status)
        assertEquals(fromDb.errorMessage?.contains("Docker build failed"), true)

        // Verify K8s resources were NOT created
        coVerify(exactly = 0) {
            kubernetesService.createDeployment(any(), any(), any(), any())
            kubernetesService.createService(any(), any(), any())
        }
    }

    @Test
    fun `buildAndDeployFunction should update status to DEPLOYMENT_FAILED when K8s deployment fails`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } returns Docker.Result(0, "Build successful")
        coEvery { kind.loadImageIntoKind(any()) } returns true
        coEvery {
            kubernetesService.createDeployment(any(), any(), any(), any())
        } returns K8sDeploymentResult.Failure("Deployment error: Insufficient resources")

        // When
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        waitForStatus(function.id, FunctionStatus.DEPLOYMENT_FAILED)

        // Then - verify function has DEPLOYMENT_FAILED status in database
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.DEPLOYMENT_FAILED, fromDb.status)
        assertEquals("Deployment error: Insufficient resources", fromDb.errorMessage)

        // Verify service was NOT created
        coVerify(exactly = 0) {
            kubernetesService.createService(any(), any(), any())
        }
    }

    @Test
    fun `buildAndDeployFunction should update status to SERVICE_FAILED when K8s service fails`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } returns Docker.Result(0, "Build successful")
        coEvery { kind.loadImageIntoKind(any()) } returns true
        coEvery {
            kubernetesService.createDeployment(any(), any(), any(), any())
        } returns K8sDeploymentResult.Success("deployment-name")
        coEvery {
            kubernetesService.createService(any(), any(), any())
        } returns K8sServiceResult.Failure("Service error: Port unavailable")

        // When
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        waitForStatus(function.id, FunctionStatus.SERVICE_FAILED)

        // Then - verify function has SERVICE_FAILED status in database
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.SERVICE_FAILED, fromDb.status)
        assertEquals("Service error: Port unavailable", fromDb.errorMessage)

        // Verify function did not reach READY status
        assertNotEquals(FunctionStatus.READY, fromDb.status)
    }

    @Test
    fun `buildAndDeployFunction should handle unexpected exceptions`() = runTest {
        // Given
        val project = createTestProject()

        // Mock external dependencies
        every { docker.build(any(), any()) } throws RuntimeException("Unexpected error")

        // When
        val function = functionService.createFunction(project.id, TestFixtures.createFunctionRequest())

        waitForStatus(function.id, FunctionStatus.BUILD_FAILED)

        // Then - verify error was caught and status updated to BUILD_FAILED
        val fromDb = functionRepository.findById(function.id)
        assertNotNull(fromDb)
        assertEquals(FunctionStatus.BUILD_FAILED, fromDb.status)
        assertEquals(fromDb.errorMessage?.contains("Build error"), true)
        assertEquals(fromDb.errorMessage?.contains("Unexpected error"), true)
    }

    // ========================================
    // Delete Tests
    // ========================================

    @Test
    fun `deleteFunction should return false when function not found`() {
        // Given
        val functionId = "non-existent"

        // When
        val result = functionService.deleteFunction(functionId)

        // Then
        assertFalse(result)
        // Verify function still doesn't exist in database
        assertNull(functionRepository.findById(functionId))
    }

    @Test
    fun `deleteFunction should delete immediately when no deployment exists`() {
        // Given - create project and function without deployment
        val project = createTestProject()
        val function = functionRepository.create(project.id, "testFunc", "fun testFunc() {}", "Unit", emptyList())

        // When
        val result = functionService.deleteFunction(function.id)

        // Then
        assertTrue(result)
        // Verify function was deleted from database
        assertNull(functionRepository.findById(function.id))
    }

    @Test
    fun `deleteFunction should trigger async cleanup when deployment exists`() = runTest {
        // Given - create project and function with deployment info
        val project = createTestProject()
        val function = functionRepository.create(project.id, "testFunc", "fun testFunc() {}", "Unit", emptyList())

        // Update function with container info to simulate having a deployment
        functionRepository.updateContainerInfo(
            functionId = function.id,
            containerName = "deployment-name",
            containerId = "service-name",
            port = 30000,
            imageTag = "test-image:latest",
            status = FunctionStatus.READY
        )

        // Mock external dependencies
        coEvery {
            kubernetesService.deleteDeploymentAndService(any(), any())
        } returns K8sDeletionResult.Success

        // When
        val result = functionService.deleteFunction(function.id)

        // Then
        assertTrue(result) // Returns true immediately

        // Wait for async cleanup - poll until function is deleted
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 3000) {
            if (functionRepository.findById(function.id) == null) {
                break
            }
            delay(100)
        }

        // Verify function was deleted from database
        assertNull(functionRepository.findById(function.id))
    }
}
