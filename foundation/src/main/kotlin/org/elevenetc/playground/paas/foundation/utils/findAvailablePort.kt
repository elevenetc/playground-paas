package org.elevenetc.playground.paas.foundation.utils


/**
 * Finds an available port on localhost.
 */
internal fun findAvailablePort(): Int {
    java.net.ServerSocket(0).use { socket ->
        return socket.localPort
    }
}