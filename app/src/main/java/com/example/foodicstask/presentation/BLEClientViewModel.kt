package com.example.foodicstask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodicstask.domain.bluetooth.BLEClientController
import com.example.foodicstask.domain.model.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BLEClientViewModel @Inject constructor(
    private val client: BLEClientController
) : ViewModel() {

    private val _uiState = MutableStateFlow(BLEClientUIState())
    val uiState = combine(
        client.foundDevices,
        _uiState
    ) { foundDevices, state ->
        state.copy(foundDevices = foundDevices)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _uiState.value
    )

    init {
        client.isScanning.onEach { scanning ->
            _uiState.update { it.copy(isScanning = scanning) }
        }.launchIn(viewModelScope)

        client.isConnected.onEach { connected ->
            _uiState.update { it.copy(isConnected = connected) }
        }.launchIn(viewModelScope)
    }

    fun startScanning() {
        client.startScanning()
    }

    fun stopScanning() {
        client.stopScanning()
    }

    fun connectToDevice(device: Device) {
        client.connectToDevice(device)
    }

    fun disconnectFromDevice() {
        client.disconnectFromDevice()
    }
}

data class BLEClientUIState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val foundDevices: List<Device> = emptyList(),
)
