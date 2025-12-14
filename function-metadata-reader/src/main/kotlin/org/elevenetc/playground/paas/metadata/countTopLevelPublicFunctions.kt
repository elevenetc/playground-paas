package org.elevenetc.playground.paas.metadata

import org.elevenetc.playground.paas.metadata.utils.withKtFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Returns the count of top-level public functions in source code.
 */
fun countTopLevelPublicFunctions(sourceCode: String): Int {
    return withKtFile(sourceCode) { ktFile ->
        val topLevelFunctions = ktFile.declarations.filterIsInstance<KtNamedFunction>()

        val publicTopLevelFunctions = topLevelFunctions.filter { function ->
            !function.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                    !function.hasModifier(KtTokens.PROTECTED_KEYWORD) &&
                    !function.hasModifier(KtTokens.INTERNAL_KEYWORD)
        }

        publicTopLevelFunctions.size
    }
}