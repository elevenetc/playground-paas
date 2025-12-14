package org.elevenetc.playground.paas.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

class CountTopLevelPublicFunctionsTest {

    @Test
    fun `returns 0 for no functions`() {
        val sourceCode = """
            val x = 42
        """.trimIndent()

        assertEquals(0, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 1 for single public function`() {
        val sourceCode = """
            fun hello(): String {
                return "Hello"
            }
        """.trimIndent()

        assertEquals(1, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 2 for multiple public functions`() {
        val sourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }

            fun subtract(a: Int, b: Int): Int {
                return a - b
            }
        """.trimIndent()

        assertEquals(2, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 1 when single public function and one private function`() {
        val sourceCode = """
            fun publicFunction(): String {
                return "public"
            }

            private fun privateFunction(): String {
                return "private"
            }
        """.trimIndent()

        assertEquals(1, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 1 when single public function and one internal function`() {
        val sourceCode = """
            fun publicFunction(): String {
                return "public"
            }

            internal fun internalFunction(): String {
                return "internal"
            }
        """.trimIndent()

        assertEquals(1, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 0 when only class public function defined`() {
        val sourceCode = """
            class MyClass {
                fun publicFunction(): String {
                    return "public"
                }

                protected fun protectedFunction(): String {
                    return "protected"
                }
            }
        """.trimIndent()

        assertEquals(0, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 0 when only private functions`() {
        val sourceCode = """
            private fun privateFunction1(): String {
                return "private1"
            }

            private fun privateFunction2(): String {
                return "private2"
            }
        """.trimIndent()

        assertEquals(0, countTopLevelPublicFunctions(sourceCode))
    }

    @Test
    fun `returns 1 for public function with nested function`() {
        val sourceCode = """
            fun outerFunction(): String {
                fun innerPrivateFunction(): String {
                    return "inner"
                }
                return innerPrivateFunction()
            }
        """.trimIndent()

        // Nested functions are considered separate functions
        assertEquals(1, countTopLevelPublicFunctions(sourceCode))
    }
}
