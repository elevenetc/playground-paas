package org.elevenetc.playground.paas.foundation.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.elevenetc.playground.paas.foundation.models.FunctionStatus

data class FunctionStatusEvent(
    val functionId: String,
    val status: FunctionStatus,
    val errorMessage: String? = null
)

class FunctionStatusEventBus {
    private val _events = MutableSharedFlow<FunctionStatusEvent>(replay = 0)
    val events: SharedFlow<FunctionStatusEvent> = _events.asSharedFlow()

    suspend fun emit(event: FunctionStatusEvent) {
        _events.emit(event)
    }
}
