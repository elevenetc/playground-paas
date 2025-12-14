package org.elevenetc.playground.paas.metadata.utils

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

fun createKotlinEnvironment(disposable: Disposable): KotlinCoreEnvironment {
    val configuration = CompilerConfiguration()
    return KotlinCoreEnvironment.createForProduction(
        disposable,
        configuration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
}

fun createKtFile(sourceCode: String, environment: KotlinCoreEnvironment): KtFile {
    val virtualFile = LightVirtualFile("temp.kt", KotlinFileType.INSTANCE, sourceCode)
    val psiFile = PsiManager.getInstance(environment.project).findFile(virtualFile)
    return psiFile as KtFile
}

inline fun <T> withKtFile(sourceCode: String, block: (KtFile) -> T): T {
    val disposable = Disposer.newDisposable()
    try {
        val environment = createKotlinEnvironment(disposable)
        val ktFile = createKtFile(sourceCode, environment)
        return block(ktFile)
    } finally {
        Disposer.dispose(disposable)
    }
}
