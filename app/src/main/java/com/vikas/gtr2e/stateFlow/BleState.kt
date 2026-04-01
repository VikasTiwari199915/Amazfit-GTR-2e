package com.vikas.gtr2e.stateFlow

data class BleState(
    val isConnected: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isBusy: Boolean = false,
    val heartRate: Int = 0,
    val commandQueueSize: Int = 0
)