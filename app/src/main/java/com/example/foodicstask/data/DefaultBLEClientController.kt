package com.example.foodicstask.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.foodicstask.data.model.toDomainDevice
import com.example.foodicstask.domain.bluetooth.BLEClientController
import com.example.foodicstask.domain.model.Device
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@SuppressLint("MissingPermission")
class DefaultBLEClientController @Inject constructor(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) : BLEClientController {

    private val bluetoothScanner: BluetoothLeScanner? = bluetoothManager.adapter.bluetoothLeScanner

    private val _isScanning = MutableStateFlow(false)
    override val isScanning = _isScanning.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected = _isConnected.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<Device>>(emptyList())
    override val foundDevices = _foundDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 20, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private val realBluetoothDevices = hashMapOf<Device, BluetoothDevice>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return

            val device = result.device.toDomainDevice()
            if (!_foundDevices.value.contains(device)) {
                _foundDevices.update { it + device }
                realBluetoothDevices[device] = result.device
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _errors.tryEmit("Scan failed with error code $errorCode")
            _isScanning.value = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            _isConnected.value = newState == BluetoothGatt.STATE_CONNECTED
        }
    }

    override fun startScanning() {
        checkBluetoothEnabledOr { return }
        checkScanPermissionsGrantedOr { return }

        bluetoothScanner?.startScan(scanCallback)
        _isScanning.value = true
    }

    override fun stopScanning() {
        checkBluetoothEnabledOr { return }
        checkScanPermissionsGrantedOr { return }

        bluetoothScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    override fun connectToDevice(device: Device) {
        checkBluetoothEnabledOr { return }
        checkConnectPermissionsGrantedOr { return }

        gatt = realBluetoothDevices[device]?.connectGatt(context, false, gattCallback)
    }

    override fun disconnectFromDevice() {
        checkBluetoothEnabledOr { return }
        checkConnectPermissionsGrantedOr { return }

        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private inline fun checkBluetoothEnabledOr(action: () -> Unit) {
        if (bluetoothManager.adapter?.isEnabled != true) {
            _errors.tryEmit("Bluetooth is turned off! turn it on & try again")
            action()
        }
    }

    /**
     * Check if we have the required permissions to scan for other bluetooth device or invoke [action] if we don't.
     */
    private inline fun checkScanPermissionsGrantedOr(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasPermission(Manifest.permission.BLUETOOTH_SCAN))
            action()
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && LegacyPermissions.any { !hasPermission(it) })
            action()
    }

    /**
     * Check if we have the required permissions to connect to other bluetooth device or invoke [action] if we don't.
     */
    private inline fun checkConnectPermissionsGrantedOr(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
            action()
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && LegacyPermissions.any { !hasPermission(it) })
            action()
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        // Legacy permissions required for Android versions below S (API 31).
        private val LegacyPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
