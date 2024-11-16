package com.example.foodicstask.presentation

import androidx.compose.runtime.Stable
import com.example.foodicstask.domain.model.Device
import com.example.foodicstask.domain.model.Message

@Stable
data class BluetoothUiState(
    val scannedDevices: List<Device>,
    val scanningRemainingTime: Int?,
    val pairedDevices: List<Device>,
    val isBluetoothEnabled: Boolean,
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val messages: List<Message>
) {
    companion object {
        val Default = BluetoothUiState(emptyList(), null, emptyList(), isBluetoothEnabled = true, isConnected = false, isConnecting = false, messages = emptyList())
    }
}
