package org.elevenetc.playground.paas.foundation.utils


/**
 * Generates standardized container/image names for functions.
 * Format: playground-paas-function-{project-name}-{function-name}
 */
internal fun generateContainerName(projectName: String, functionName: String): String {
    val sanitizedProject = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
    val sanitizedFunction = functionName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
    return "playground-paas-function-$sanitizedProject-$sanitizedFunction"
}