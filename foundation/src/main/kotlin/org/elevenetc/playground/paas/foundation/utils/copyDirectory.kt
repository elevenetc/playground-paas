package org.elevenetc.playground.paas.foundation.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption


internal fun copyDirectory(source: File, target: File) {
    if (source.isDirectory) {
        if (!target.exists()) {
            target.mkdirs()
        }
        source.listFiles()?.forEach { file ->
            copyDirectory(file, File(target, file.name))
        }
    } else {
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}