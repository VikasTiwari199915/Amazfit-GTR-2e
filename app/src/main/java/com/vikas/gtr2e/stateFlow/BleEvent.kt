package com.vikas.gtr2e.stateFlow

sealed class BleEvent {

    data class WatchFaceSet(val success: Boolean) : BleEvent()

    data class WatchFacesFetched(val ids: List<Int>) : BleEvent()

    data class HeartRateChanged(val value: Int) : BleEvent()

    data class ConnectionChanged(val connected: Boolean) : BleEvent()

    data class CommandResult(val success: Boolean) : BleEvent()
}