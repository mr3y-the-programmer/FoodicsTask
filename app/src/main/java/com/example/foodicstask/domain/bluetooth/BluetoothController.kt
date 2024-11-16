package com.example.foodicstask.domain.bluetooth

import com.example.foodicstask.domain.model.ConnectionResult
import com.example.foodicstask.domain.model.Device
import com.example.foodicstask.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val isEnabled: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<Device>>
    val pairedDevices: StateFlow<List<Device>>
    val errors: SharedFlow<String>
    val scanningRemainingTimeInSec: StateFlow<Int?>

    fun startDeviceDiscovery()

    fun updatePairedDevices()

    fun stopDeviceDiscovery()

    fun startBluetoothServer(): Flow<ConnectionResult>

    fun connectToDevice(device: Device): Flow<ConnectionResult>

    suspend fun sendMessage(text: String): Message?

    fun closeConnection()

    fun release()
}
