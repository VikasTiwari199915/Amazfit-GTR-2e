package com.vikas.gtr2e.stateFlow

import androidx.lifecycle.ViewModel

class GTRViewModel(private val repo: GTRRepository) : ViewModel() {

    val state = repo.state              // StateFlow<BleState>
    val events = repo.events            // SharedFlow<BleEvent>

    fun setWatchFace(id: Int) = repo.setWatchFace(id)
    fun fetchWatchFaces() = repo.fetchWatchFaces()
}