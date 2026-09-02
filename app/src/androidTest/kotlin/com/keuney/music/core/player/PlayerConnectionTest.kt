package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerConnectionTest {
    @Test
    fun repeatedConnectDisconnectAndPendingCancellationAreSafe() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync {
                connection.disconnect()
                connection.connect()
                connection.disconnect()
                assertEquals(ConnectionState.Disconnected, connection.state.value)
                connection.connect()
                connection.connect()
            }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync {
                connection.disconnect()
                connection.disconnect()
                assertEquals(ConnectionState.Disconnected, connection.state.value)
                connection.connect()
            }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
        } finally {
            instrumentation.runOnMainSync { connection.disconnect() }
        }
        assertEquals(ConnectionState.Disconnected, connection.state.value)
    }
}
