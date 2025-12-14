package org.elevenetc.playground.paas.metadata

import kotlinx.serialization.Serializable

@Serializable
data class FunctionMetadata(
    val name: String,
    val parameters: List<ParameterMetadata>,
    val returnType: TypeMetadata
)

@Serializable
data class ParameterMetadata(
    val name: String,
    val type: TypeMetadata,
    val defaultValue: ValueMetadata?,
    val isVararg: Boolean
)

@Serializable
data class TypeMetadata(
    val className: String,
    val nullable: Boolean
)

@Serializable
data class ValueMetadata(
    val value: String
)
