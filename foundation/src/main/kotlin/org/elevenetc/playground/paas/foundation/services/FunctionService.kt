package org.elevenetc.playground.paas.foundation.services

import org.elevenetc.playground.paas.foundation.compiler.extractFunctionName
import org.elevenetc.playground.paas.foundation.compiler.extractParameters
import org.elevenetc.playground.paas.foundation.compiler.extractReturnType
import org.elevenetc.playground.paas.foundation.models.CreateFunctionRequest
import org.elevenetc.playground.paas.foundation.models.Function
import org.elevenetc.playground.paas.foundation.models.UpdateFunctionRequest
import org.elevenetc.playground.paas.foundation.repositories.FunctionRepository

class FunctionService(private val functionRepository: FunctionRepository) {

    fun createFunction(projectId: String, request: CreateFunctionRequest): Function {
        // Extract name, parameters, and return type from source code
        val name = request.name ?: extractFunctionName(request.sourceCode)
        val parameters = extractParameters(request.sourceCode)
        val returnType = extractReturnType(request.sourceCode)

        return functionRepository.create(
            projectId = projectId,
            name = name,
            sourceCode = request.sourceCode,
            returnType = returnType,
            parameters = parameters
        )
    }

    fun getAllFunctions(): List<Function> {
        return functionRepository.findAll()
    }

    fun getFunctionById(id: String): Function? {
        return functionRepository.findById(id)
    }

    fun getFunctionsByProjectId(projectId: String): List<Function> {
        return functionRepository.findByProjectId(projectId)
    }

    fun updateFunction(id: String, request: UpdateFunctionRequest): Function? {
        // If source code is being updated, extract parameters and return type
        val parameters = if (request.sourceCode != null) {
            extractParameters(request.sourceCode)
        } else null

        val returnType = if (request.sourceCode != null) {
            extractReturnType(request.sourceCode)
        } else null

        return functionRepository.update(
            id = id,
            name = request.name,
            sourceCode = request.sourceCode,
            returnType = returnType,
            parameters = parameters
        )
    }

    fun deleteFunction(id: String): Boolean {
        return functionRepository.delete(id)
    }
}
