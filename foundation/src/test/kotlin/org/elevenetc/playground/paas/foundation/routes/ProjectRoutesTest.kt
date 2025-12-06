package org.elevenetc.playground.paas.foundation.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.elevenetc.playground.paas.foundation.jsonBody
import org.elevenetc.playground.paas.foundation.models.CreateProjectRequest
import org.elevenetc.playground.paas.foundation.models.Project
import org.elevenetc.playground.paas.foundation.models.UpdateProjectRequest
import org.elevenetc.playground.paas.foundation.testApp
import kotlin.test.*

class ProjectRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test create project`() = testApp {

        val response = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(
                name = "TestProject",
                description = "Test description"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val project = json.decodeFromString<Project>(response.bodyAsText())
        assertEquals("TestProject", project.name)
        assertEquals("Test description", project.description)
        assertNotNull(project.id)
    }

    @Test
    fun `test create project with duplicate name returns conflict`() = testApp {

        // Create first project
        client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "First"))
        }

        // Try to create second project with same name
        val response = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "Second"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("already exists"))
    }

    @Test
    fun `test list projects`() = testApp {

        // Create a few projects
        client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "Project1", description = "First project"))
        }
        client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "Project2", description = "Second project"))
        }

        // List all projects
        val response = client.get("/api/projects")

        assertEquals(HttpStatusCode.OK, response.status)
        val projects = json.decodeFromString<List<Project>>(response.bodyAsText())
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "Project1" })
        assertTrue(projects.any { it.name == "Project2" })
    }

    @Test
    fun `test get project by id`() = testApp {

        // Create a project
        val createResponse = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "Test description"))
        }
        val createdProject = json.decodeFromString<Project>(createResponse.bodyAsText())

        // Get project by ID
        val response = client.get("/api/projects/${createdProject.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val project = json.decodeFromString<Project>(response.bodyAsText())
        assertEquals(createdProject.id, project.id)
        assertEquals("TestProject", project.name)
    }

    @Test
    fun `test get project by non-existent id returns not found`() = testApp {

        val response = client.get("/api/projects/non-existent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"))
    }

    @Test
    fun `test update project`() = testApp {

        // Create a project
        val createResponse = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "Original description"))
        }
        val createdProject = json.decodeFromString<Project>(createResponse.bodyAsText())

        // Update the project
        val response = client.put("/api/projects/${createdProject.id}") {
            jsonBody(UpdateProjectRequest(name = "UpdatedProject", description = "Updated description"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedProject = json.decodeFromString<Project>(response.bodyAsText())
        assertEquals(createdProject.id, updatedProject.id)
        assertEquals("UpdatedProject", updatedProject.name)
        assertEquals("Updated description", updatedProject.description)
        assertNotEquals(createdProject.updatedAt, updatedProject.updatedAt)
    }

    @Test
    fun `test update project with partial data`() = testApp {

        // Create a project
        val createResponse = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "Original description"))
        }
        val createdProject = json.decodeFromString<Project>(createResponse.bodyAsText())

        // Update only description
        val response = client.put("/api/projects/${createdProject.id}") {
            jsonBody(UpdateProjectRequest(description = "New description only"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedProject = json.decodeFromString<Project>(response.bodyAsText())
        assertEquals("TestProject", updatedProject.name) // Name unchanged
        assertEquals("New description only", updatedProject.description)
    }

    @Test
    fun `test update non-existent project returns not found`() = testApp {

        val response = client.put("/api/projects/non-existent-id") {
            jsonBody(UpdateProjectRequest(name = "Updated"))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test delete project`() = testApp {

        // Create a project
        val createResponse = client.post("/api/projects") {
            jsonBody(CreateProjectRequest(name = "TestProject", description = "To be deleted"))
        }
        val createdProject = json.decodeFromString<Project>(createResponse.bodyAsText())

        // Delete the project
        val deleteResponse = client.delete("/api/projects/${createdProject.id}")

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify project is deleted
        val getResponse = client.get("/api/projects/${createdProject.id}")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `test delete non-existent project returns not found`() = testApp {

        val response = client.delete("/api/projects/non-existent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test list projects returns empty array when no projects exist`() = testApp {

        val response = client.get("/api/projects")

        assertEquals(HttpStatusCode.OK, response.status)
        val projects = json.decodeFromString<List<Project>>(response.bodyAsText())
        assertTrue(projects.isEmpty())
    }
}
