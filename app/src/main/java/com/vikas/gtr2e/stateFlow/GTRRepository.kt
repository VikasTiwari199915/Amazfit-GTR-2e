package com.vikas.gtr2e.stateFlow

import android.app.usage.UsageEvents
import androidx.lifecycle.asLiveData
import com.vikas.gtr2e.services.GTR2eBleService
import kotlinx.coroutines.flow.map

class GTRRepository(private val service: GTR2eBleService) {

    val state = service.state.asLiveData()

    val events = service.events.map { UsageEvents.Event() }.asLiveData()

    fun setWatchFace(id: Int) = service.setWatchFaceWithId(id)

    fun fetchWatchFaces() = service.requestWatchFaceIdList()

    fun set24Hr(enabled: Boolean) = service.enable24HrFormatTime(enabled)
}