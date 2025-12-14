package org.elevenetc.playground.paas.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionMetadataExtractorTest {

    @Test
    fun `extract simple function data`() {
        val sourceCode = """
            fun hello()
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        assertEquals("hello", metadata[0].name)
        assertEquals(0, metadata[0].parameters.size)
        assertEquals("Unit", metadata[0].returnType.className)
        assertEquals(false, metadata[0].returnType.nullable)
    }

    @Test
    fun `extract function with parameters and return value`() {
        val sourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        val function = metadata[0]
        assertEquals("add", function.name)
        assertEquals(2, function.parameters.size)
        assertEquals("a", function.parameters[0].name)
        assertEquals("Int", function.parameters[0].type.className)
        assertEquals(false, function.parameters[0].type.nullable)
        assertEquals(null, function.parameters[0].defaultValue)
        assertEquals(false, function.parameters[0].isVararg)
        assertEquals("b", function.parameters[1].name)
        assertEquals("Int", function.parameters[1].type.className)
        assertEquals(false, function.parameters[1].type.nullable)
        assertEquals(null, function.parameters[1].defaultValue)
        assertEquals(false, function.parameters[1].isVararg)
        assertEquals("Int", function.returnType.className)
        assertEquals(false, function.returnType.nullable)
    }

    @Test
    fun `extract multiple functions from source`() {
        val sourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }

            fun subtract(x: Int, y: Int): Int {
                return x - y
            }

            fun multiply(m: Int, n: Int): Int {
                return m * n
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(3, metadata.size)
        assertEquals("add", metadata[0].name)
        assertEquals("subtract", metadata[1].name)
        assertEquals("multiply", metadata[2].name)
    }

    @Test
    fun `extract function with nullable parameter`() {
        val sourceCode = """
            fun process(value: String?): Int {
                return value?.length ?: 0
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        val function = metadata[0]
        assertEquals("process", function.name)
        assertEquals(1, function.parameters.size)
        assertEquals("value", function.parameters[0].name)
        assertEquals("String", function.parameters[0].type.className)
        assertEquals(true, function.parameters[0].type.nullable)
        assertEquals(null, function.parameters[0].defaultValue)
        assertEquals(false, function.parameters[0].isVararg)
        assertEquals("Int", function.returnType.className)
        assertEquals(false, function.returnType.nullable)
    }

    @Test
    fun `extract function with nullable return type`() {
        val sourceCode = """
            fun findValue(key: String): String? {
                return null
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        val function = metadata[0]
        assertEquals("findValue", function.name)
        assertEquals("String", function.returnType.className)
        assertEquals(true, function.returnType.nullable)
    }

    @Test
    fun `extract function with vararg parameter`() {
        val sourceCode = """
            fun concatenate(vararg strings: String): String {
                return strings.joinToString()
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        val function = metadata[0]
        assertEquals("concatenate", function.name)
        assertEquals(1, function.parameters.size)
        assertEquals("strings", function.parameters[0].name)
        assertEquals("String", function.parameters[0].type.className)
        assertEquals(false, function.parameters[0].type.nullable)
        assertEquals(null, function.parameters[0].defaultValue)
        assertEquals(true, function.parameters[0].isVararg)
    }

    @Test
    fun `extract function with default parameter value`() {
        val sourceCode = $$"""
            fun greet(name: String = "World"): String {
                return "Hello, $name"
            }
        """.trimIndent()

        val metadata = readFunctionMetadata(sourceCode)

        assertEquals(1, metadata.size)
        val function = metadata[0]
        assertEquals("greet", function.name)
        assertEquals(1, function.parameters.size)
        assertEquals("name", function.parameters[0].name)
        assertEquals("String", function.parameters[0].type.className)
        assertEquals(false, function.parameters[0].type.nullable)
        assertEquals("\"World\"", function.parameters[0].defaultValue?.value)
        assertEquals(false, function.parameters[0].isVararg)
        assertEquals("String", function.returnType.className)
        assertEquals(false, function.returnType.nullable)
    }
}
