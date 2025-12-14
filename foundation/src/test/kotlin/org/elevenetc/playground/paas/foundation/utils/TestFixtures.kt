package org.elevenetc.playground.paas.foundation.utils

import org.elevenetc.playground.paas.foundation.models.*
import org.elevenetc.playground.paas.foundation.models.Function
import java.time.Instant
import java.util.*

/**
 * Test fixtures for creating test data.
 * Provides factory methods for creating test objects with sensible defaults.
 */
object TestFixtures {

    /**
     * Create a test project with default values.
     */
    fun createProject(
        id: String = UUID.randomUUID().toString(),
        name: String = "test-project",
        description: String? = "Test project description",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = Project(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Create a test function with default values.
     */
    fun createFunction(
        id: String = UUID.randomUUID().toString(),
        projectId: String = UUID.randomUUID().toString(),
        name: String = "testFunction",
        sourceCode: String = "fun testFunction(): String = \"Hello, World!\"",
        returnType: String = "String",
        parameters: List<FunctionParameter> = emptyList(),
        status: FunctionStatus = FunctionStatus.PENDING,
        containerName: String? = null,
        containerId: String? = null,
        port: Int? = null,
        imageTag: String? = null,
        errorMessage: String? = null,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = Function(
        id = id,
        projectId = projectId,
        name = name,
        sourceCode = sourceCode,
        returnType = returnType,
        parameters = parameters,
        status = status,
        containerName = containerName,
        containerId = containerId,
        port = port,
        imageTag = imageTag,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Create a function parameter.
     */
    fun createParameter(
        name: String = "param",
        type: String = "String"
    ) = FunctionParameter(
        name = name,
        type = type
    )

    /**
     * Create a CreateFunctionRequest.
     */
    fun createFunctionRequest(
        sourceCode: String = "fun testFunction(): String = \"Hello, World!\""
    ) = CreateFunctionRequest(
        sourceCode = sourceCode
    )
}
