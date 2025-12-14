package org.elevenetc.playground.paas.metadata

import org.elevenetc.playground.paas.metadata.utils.withKtFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isPublic

fun readFunctionMetadata(sourceCode: String): List<FunctionMetadata> {
    return withKtFile(sourceCode) { ktFile ->
        extractFunctionsFromKtFile(ktFile)
    }
}

private fun extractFunctionsFromKtFile(ktFile: KtFile): List<FunctionMetadata> {
    val functions = ktFile.collectDescendantsOfType<KtNamedFunction>()
    return functions.filter { it.isPublic }.map { function ->
        FunctionMetadata(
            name = function.name ?: "unknown",
            parameters = extractParameters(function),
            returnType = extractReturnType(function)
        )
    }
}

private fun extractParameters(function: KtNamedFunction): List<ParameterMetadata> {
    return function.valueParameters.map { param ->
        ParameterMetadata(
            name = param.name ?: "unknown",
            type = parseType(param.typeReference?.text ?: "Unknown"),
            defaultValue = param.defaultValue?.text?.let { ValueMetadata(it) },
            isVararg = param.isVarArg
        )
    }
}

private fun extractReturnType(function: KtNamedFunction): TypeMetadata {
    return parseType(function.typeReference?.text ?: "Unit")
}

private fun parseType(typeText: String): TypeMetadata {
    val nullable = typeText.endsWith("?")
    val className = if (nullable) typeText.dropLast(1) else typeText
    return TypeMetadata(className = className, nullable = nullable)
}
