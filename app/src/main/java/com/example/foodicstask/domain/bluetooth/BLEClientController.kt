package com.example.foodicstask.domain.bluetooth

import com.example.foodicstask.domain.model.Device
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BLEClientController {

    val isScanning: StateFlow<Boolean>

    val isConnected: StateFlow<Boolean>

    val foundDevices: StateFlow<List<Device>>

    val errors: SharedFlow<String>

    fun startScanning()

    fun stopScanning()

    fun connectToDevice(device: Device)

    fun disconnectFromDevice()
}
