package org.elevenetc.playground.paas.foundation.services

import org.elevenetc.playground.paas.foundation.models.CreateProjectRequest
import org.elevenetc.playground.paas.foundation.models.Project
import org.elevenetc.playground.paas.foundation.models.UpdateProjectRequest
import org.elevenetc.playground.paas.foundation.repositories.ProjectRepository

class ProjectService(private val projectRepository: ProjectRepository) {

    fun createProject(request: CreateProjectRequest): Project {
        // Check if project with same name already exists
        val existing = projectRepository.findByName(request.name)
        if (existing != null) {
            throw IllegalArgumentException("Project with name '${request.name}' already exists")
        }

        return projectRepository.create(
            name = request.name,
            description = request.description
        )
    }

    fun getAllProjects(): List<Project> {
        return projectRepository.findAll()
    }

    fun getProjectById(id: String): Project? {
        return projectRepository.findById(id)
    }

    fun updateProject(id: String, request: UpdateProjectRequest): Project? {
        return projectRepository.update(
            id = id,
            name = request.name,
            description = request.description
        )
    }

    fun deleteProject(id: String): Boolean {
        return projectRepository.delete(id)
    }

    fun projectExists(id: String): Boolean {
        return projectRepository.findById(id) != null
    }
}
