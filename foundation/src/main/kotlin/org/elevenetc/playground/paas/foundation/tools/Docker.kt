package org.elevenetc.playground.paas.foundation.tools

import java.io.File

class Docker {
    fun build(
        imageName: String,
        directory: File
    ): Result {
        val processBuilder = ProcessBuilder(
            "docker", "build", "-t", imageName, "."
        )
        processBuilder.directory(directory)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return Result(exitCode, output)
    }

    data class Result(val exitCode: Int, val output: String)
}