package com.example.foodicstask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodicstask.domain.model.ConnectionResult
import com.example.foodicstask.domain.bluetooth.BluetoothController
import com.example.foodicstask.domain.model.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState.Default)
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            messages = if (state.isConnected) state.messages else emptyList()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _state.value
    )
    val errors = bluetoothController.errors

    private var deviceConnectionJob: Job? = null

    init {
        bluetoothController.isConnected.onEach { connected ->
            _state.update { it.copy(isConnected = connected) }
        }.launchIn(viewModelScope)

        bluetoothController.isEnabled.onEach { enabled ->
            _state.update { it.copy(isBluetoothEnabled = enabled) }
        }.launchIn(viewModelScope)

        bluetoothController.scanningRemainingTimeInSec.onEach { remainingSecs ->
            _state.update { it.copy(scanningRemainingTime = remainingSecs) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: Device) {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun waitForIncomingConnections() {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            val newMessage = bluetoothController.sendMessage(message)
            if (newMessage != null) {
                _state.update {
                    it.copy(
                        messages = it.messages + newMessage
                    )
                }
            }
        }
    }

    fun startScan() {
        bluetoothController.startDeviceDiscovery()
    }

    fun updatePairedDevices() {
        bluetoothController.updatePairedDevices()
    }

    fun stopScan() {
        bluetoothController.stopDeviceDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                is ConnectionResult.ConnectionEstablished ->
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false
                        )
                    }
                is ConnectionResult.TransferSucceeded ->
                    _state.update {
                        it.copy(
                            messages = it.messages + result.message
                        )
                    }
                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false
                        )
                    }
                }
            }
        }.catch { _ ->
            bluetoothController.closeConnection()
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                )
            }
        }.launchIn(viewModelScope)
    }
}
