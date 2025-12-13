package org.elevenetc.playground.paas.foundation.utils


/**
 * Generates standardized Kubernetes resource names for functions.
 * Format: paas-func-{project-name}-{function-name}
 * (Must be DNS-1123 compliant: lowercase alphanumeric + hyphens, max 63 chars)
 */
internal fun generateKubResourceName(projectName: String, functionName: String): String {
    val sanitizedProject = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
    val sanitizedFunction = functionName.lowercase().replace(Regex("[^a-z0-9-]"), "-")
    val name = "paas-func-$sanitizedProject-$sanitizedFunction"

    // Kubernetes name limit is 63 characters
    return if (name.length > 63) {
        name.take(63).trimEnd('-')
    } else {
        name
    }
}