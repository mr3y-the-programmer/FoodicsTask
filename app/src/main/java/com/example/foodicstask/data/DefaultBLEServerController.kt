package com.example.foodicstask.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.foodicstask.domain.bluetooth.BLEServerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingPermission", "InlinedApi")
class DefaultBLEServerController @Inject constructor(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) : BLEServerController {

    private var server: BluetoothGattServer? = null
    private var ctfService: BluetoothGattService? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private val isServerListening: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    override suspend fun startServer() {
        checkBluetoothEnabledOr { return }
        checkConnectPermissionsGrantedOr { return }

        withContext(Dispatchers.IO) {
            //If server already exists, we don't need to create one
            if (server != null) {
                return@withContext
            }

            startHandlingIncomingConnections()
            startAdvertising()
        }
    }

    override suspend fun stopServer() {
        checkBluetoothEnabledOr { return }
        checkConnectPermissionsGrantedOr { return }

        withContext(Dispatchers.IO) {
            //if no server, nothing to do
            if (server == null) {
                return@withContext
            }

            stopAdvertising()
            stopHandlingIncomingConnections()
        }
    }

    private suspend fun startAdvertising() {
        val advertiser: BluetoothLeAdvertiser? = bluetoothManager.adapter.bluetoothLeAdvertiser

        // if already advertising, ignore
        if (advertiseCallback != null) {
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .build()

        advertiseCallback = suspendCoroutine { continuation ->
            val advertiseCallback = object: AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)
                    continuation.resume(this)
                }

                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    throw Exception("Unable to start advertising, errorCode: $errorCode")
                }
            }
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        }
    }

    private fun stopAdvertising() {
        val advertiser: BluetoothLeAdvertiser? = bluetoothManager.adapter.bluetoothLeAdvertiser

        //if not currently advertising, ignore
        advertiseCallback?.let {
            advertiser?.stopAdvertising(it)
            advertiseCallback = null
        }
    }

    private fun startHandlingIncomingConnections() {
        server = bluetoothManager.openGattServer(context, object: BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                super.onServiceAdded(status, service)
                isServerListening.value = true
            }
        })

        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        server?.addService(service)
        ctfService = service
    }

    private fun stopHandlingIncomingConnections() {
        ctfService?.let {
            server?.removeService(it)
            ctfService = null
        }
    }

    private inline fun checkBluetoothEnabledOr(action: () -> Unit) {
        if (bluetoothManager.adapter?.isEnabled != true) {
            action()
        }
    }

    /**
     * Check if we have the required permissions to connect to other bluetooth device or invoke [action] if we don't.
     */
    private inline fun checkConnectPermissionsGrantedOr(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ModernPermissions.any { !hasPermission(it) })
            action()
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && LegacyPermissions.any { !hasPermission(it) })
            action()
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val serviceUuid = UUID.fromString("8c380000-10bd-4fdb-ba21-1922d6cf860d")
        private val ModernPermissions = arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        // Legacy permissions required for Android versions below S (API 31).
        private val LegacyPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}