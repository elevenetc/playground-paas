package org.elevenetc.playground.paas.foundation.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.jsonBody
import org.elevenetc.playground.paas.foundation.models.*
import org.elevenetc.playground.paas.foundation.testApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.elevenetc.playground.paas.foundation.models.Function as FunctionModel

class FunctionRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun ApplicationTestBuilder.createTestProject(): Project {
        val response = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "Test project for functions"))
        }
        return json.decodeFromString<Project>(response.bodyAsText())
    }

    @Test
    fun `test create function`() = testApp {

        val project = createTestProject()

        val response = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val function = json.decodeFromString<FunctionModel>(response.bodyAsText())
        assertEquals("add", function.name)
        assertEquals("fun add(a: Int, b: Int): Int = a + b", function.sourceCode)
        assertEquals("Int", function.returnType)
        assertEquals(2, function.parameters.size)
        assertEquals(FunctionStatus.PENDING, function.status)
        assertEquals(project.id, function.projectId)
    }

    @Test
    fun `test create function for non-existent project returns not found`() = testApp {

        val response = client.post("/api/projects/non-existent-id/functions") {
            jsonBody(
                CreateFunctionRequest(
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Project not found"))
    }

    @Test
    fun `test list functions for project`() = testApp {

        val project = createTestProject()

        // Create multiple functions
        client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }

        client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "multiply",
                    sourceCode = "fun multiply(a: Int, b: Int): Int = a * b"
                )
            )
        }

        // List functions
        val response = client.get("/api/projects/${project.id}/functions")

        assertEquals(HttpStatusCode.OK, response.status)
        val functions = json.decodeFromString<List<FunctionModel>>(response.bodyAsText())
        assertEquals(2, functions.size)
        assertTrue(functions.any { it.name == "add" })
        assertTrue(functions.any { it.name == "multiply" })
    }

    @Test
    fun `test list all functions across projects`() = testApp {

        val project1 = createTestProject()

        // Create project 2
        val project2Response = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "Project2", description = "Second project"))
        }
        val project2 = json.decodeFromString<Project>(project2Response.bodyAsText())

        // Create function in project 1
        client.post("/api/projects/${project1.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "func1",
                    sourceCode = "fun func1(): Int = 1"
                )
            )
        }

        // Create function in project 2
        client.post("/api/projects/${project2.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "func2",
                    sourceCode = "fun func2(): Int = 2"
                )
            )
        }

        // List all functions
        val response = client.get("/api/functions")

        assertEquals(HttpStatusCode.OK, response.status)
        val functions = json.decodeFromString<List<FunctionModel>>(response.bodyAsText())
        assertEquals(2, functions.size)
        assertTrue(functions.any { it.projectId == project1.id })
        assertTrue(functions.any { it.projectId == project2.id })
    }

    @Test
    fun `test get function by id`() = testApp {

        val project = createTestProject()

        // Create a function
        val createResponse = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }
        val createdFunction = json.decodeFromString<FunctionModel>(createResponse.bodyAsText())

        // Get function by ID (project-scoped)
        val response = client.get("/api/projects/${project.id}/functions/${createdFunction.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val function = json.decodeFromString<FunctionModel>(response.bodyAsText())
        assertEquals(createdFunction.id, function.id)
        assertEquals("add", function.name)
    }

    @Test
    fun `test get function by id without project scope`() = testApp {

        val project = createTestProject()

        // Create a function
        val createResponse = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }
        val createdFunction = json.decodeFromString<FunctionModel>(createResponse.bodyAsText())

        // Get function by ID (global endpoint)
        val response = client.get("/api/functions/${createdFunction.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val function = json.decodeFromString<FunctionModel>(response.bodyAsText())
        assertEquals(createdFunction.id, function.id)
    }

    @Test
    fun `test get non-existent function returns not found`() = testApp {

        val project = createTestProject()

        val response = client.get("/api/projects/${project.id}/functions/non-existent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"))
    }

    @Test
    fun `test update function`() = testApp {

        val project = createTestProject()

        // Create a function
        val createResponse = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }
        val createdFunction = json.decodeFromString<FunctionModel>(createResponse.bodyAsText())

        // Update the function
        val response = client.put("/api/projects/${project.id}/functions/${createdFunction.id}") {
            jsonBody(
                UpdateFunctionRequest(
                    name = "addOptimized",
                    sourceCode = "fun addOptimized(x: Int, y: Int): Int = x + y"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedFunction = json.decodeFromString<FunctionModel>(response.bodyAsText())
        assertEquals(createdFunction.id, updatedFunction.id)
        assertEquals("addOptimized", updatedFunction.name)
        assertEquals("fun addOptimized(x: Int, y: Int): Int = x + y", updatedFunction.sourceCode)
        assertEquals(FunctionStatus.PENDING, updatedFunction.status) // Status reset to PENDING when code changes
    }

    @Test
    fun `test update function with partial data`() = testApp {

        val project = createTestProject()

        // Create a function
        val createResponse = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "add",
                    sourceCode = "fun add(a: Int, b: Int): Int = a + b"
                )
            )
        }
        val createdFunction = json.decodeFromString<FunctionModel>(createResponse.bodyAsText())

        // Update only name
        val response = client.put("/api/projects/${project.id}/functions/${createdFunction.id}") {
            jsonBody(UpdateFunctionRequest(name = "addNumbers"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedFunction = json.decodeFromString<FunctionModel>(response.bodyAsText())
        assertEquals("addNumbers", updatedFunction.name)
        assertEquals(createdFunction.sourceCode, updatedFunction.sourceCode) // Source code unchanged
    }

    @Test
    fun `test update non-existent function returns not found`() = testApp {

        val project = createTestProject()

        val response = client.put("/api/projects/${project.id}/functions/non-existent-id") {
            jsonBody(UpdateFunctionRequest(name = "updated"))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test delete function`() = testApp {

        val project = createTestProject()

        // Create a function
        val createResponse = client.post("/api/projects/${project.id}/functions") {
            jsonBody(
                CreateFunctionRequest(
                    name = "toDelete",
                    sourceCode = "fun toDelete(): Unit = Unit"
                )
            )
        }
        val createdFunction = json.decodeFromString<FunctionModel>(createResponse.bodyAsText())

        // Delete the function
        val deleteResponse = client.delete("/api/projects/${project.id}/functions/${createdFunction.id}")

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify function is deleted
        val getResponse = client.get("/api/functions/${createdFunction.id}")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `test delete non-existent function returns not found`() = testApp {

        val project = createTestProject()

        val response = client.delete("/api/projects/${project.id}/functions/non-existent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test list functions returns empty array when no functions exist`() = testApp {

        val project = createTestProject()

        val response = client.get("/api/projects/${project.id}/functions")

        assertEquals(HttpStatusCode.OK, response.status)
        val functions = json.decodeFromString<List<FunctionModel>>(response.bodyAsText())
        assertTrue(functions.isEmpty())
    }
}
