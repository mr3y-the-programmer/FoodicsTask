package com.example.foodicstask.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.foodicstask.data.chat.DataTransferService
import com.example.foodicstask.domain.model.ConnectionResult
import com.example.foodicstask.data.model.SessionCancelledException
import com.example.foodicstask.data.model.toByteArray
import com.example.foodicstask.data.model.toDomainDevice
import com.example.foodicstask.data.pair.PairingStatusReceiver
import com.example.foodicstask.data.scan.FoundDevicesReceiver
import com.example.foodicstask.data.scan.ScanningCountdownTimer
import com.example.foodicstask.domain.bluetooth.BluetoothController
import com.example.foodicstask.domain.model.Device
import com.example.foodicstask.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class DefaultBluetoothController @Inject constructor(
    // make sure to inject application context here NOT activity context because the latter would cause memory leaks.
    private val context: Context,
    bluetoothManager: BluetoothManager
) : BluetoothController {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<Device>>(emptyList())
    override val scannedDevices: StateFlow<List<Device>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<Device>>(emptyList())
    override val pairedDevices: StateFlow<List<Device>> = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 20, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _scanningRemainingTimeInSec = MutableStateFlow<Int?>(null)
    override val scanningRemainingTimeInSec: StateFlow<Int?> = _scanningRemainingTimeInSec.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _isEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null
    private var chatService: DataTransferService? = null
    private val scanningCountdownTimer = ScanningCountdownTimer(
        onTickInterval = { remainingSecs ->
            _scanningRemainingTimeInSec.update { remainingSecs }
        },
        onStop = {
            stopDeviceDiscovery()
        }
    )

    private val foundDeviceReceiver = FoundDevicesReceiver { device ->
        _scannedDevices.update { existingDevices ->
            val newDevice = device.toDomainDevice()
            if (newDevice !in existingDevices)
                existingDevices + newDevice
            else
                existingDevices
        }
    }

    private val pairingStatusReceiver = PairingStatusReceiver { isNowConnected, device ->
        if (bluetoothAdapter?.bondedDevices?.contains(device) == true) {
            _isConnected.update { isNowConnected }
        }
    }

    init {
        context.registerReceiver(
            pairingStatusReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDeviceDiscovery() {
        checkBluetoothEnabledOr { return }
        checkScanPermissionsGrantedOr { return }

        context.registerReceiver(foundDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        updatePairedDevices()

        scanningCountdownTimer.start()
        _scanningRemainingTimeInSec.update { 12 }
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDeviceDiscovery() {
        checkBluetoothEnabledOr { return }
        checkScanPermissionsGrantedOr { return }

        // if user cancelled discovery explicitly before timeout finishes
        scanningCountdownTimer.cancel()
        _scanningRemainingTimeInSec.update { null }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            checkBluetoothEnabledOr { emit(ConnectionResult.Error) }
            checkConnectPermissionsGrantedOr {
                _errors.tryEmit("Insufficient permissions! Grant the required permissions & try again.")
                emit(ConnectionResult.Error)
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "establishing_connect_service",
                UUID.fromString(CommunicationServiceUUID)
            )
            stopDeviceDiscovery() // There is no point in keeping discovery running if we're establishing a connection.

            var keepLooping = true
            var sessionIsCancelled = false
            while (keepLooping) {
                currentClientSocket = try {
                    // Keep listening for 5 minutes, we can keep listening indefinitely but that would drain device battery.
                    currentServerSocket?.accept(300_000)
                } catch (e: IOException) {
                    keepLooping = false
                    if (!sessionIsCancelled) {
                        _errors.tryEmit("Timeout! Failed to establish a connection!")
                    }
                    null
                }
                currentClientSocket?.let {
                    emit(ConnectionResult.ConnectionEstablished)
                    currentServerSocket?.close() // close the socket as we don't want to accept any more incoming connections.
                    val service = DataTransferService(it)
                    chatService = service

                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .catch { throwable ->
                                if (throwable !is SessionCancelledException) throw throwable

                                _errors.tryEmit("Communication session terminated!")
                                sessionIsCancelled = true
                            }
                            .map { message -> ConnectionResult.TransferSucceeded(message) }
                    )
                } ?: emit(ConnectionResult.Error)
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: Device): Flow<ConnectionResult> {
        return flow {
            checkBluetoothEnabledOr { emit(ConnectionResult.Error) }
            checkConnectPermissionsGrantedOr {
                _errors.tryEmit("Insufficient permissions! Grant the required permissions & try again.")
                emit(ConnectionResult.Error)
            }

            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.macAddress)
                ?.createRfcommSocketToServiceRecord(UUID.fromString(CommunicationServiceUUID))
            stopDeviceDiscovery() // There is no point in keeping discovery running if we found the device to connect with.

            currentClientSocket?.let { socket ->
                var errorMessage: String? = null
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)

                    val service = DataTransferService(socket)
                    chatService = service
                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .map { ConnectionResult.TransferSucceeded(it) }
                    )
                } catch (e: SessionCancelledException) {
                    errorMessage = "Communication session terminated!"
                } catch (e: IOException) {
                    errorMessage = "Failed to connect! make sure the device is in range & accepts your pairing request"
                }
                errorMessage?.let { message ->
                    socket.close()
                    currentClientSocket = null
                    _errors.tryEmit(message)
                    emit(ConnectionResult.Error)
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(text: String): Message? {
        checkBluetoothEnabledOr { return null }
        checkConnectPermissionsGrantedOr { return null }

        if (chatService == null)
            return null

        val message = Message(
            text = text,
            senderName = bluetoothAdapter?.name ?: "(No Name)",
            isFromCurrentUser = true
        )

        val succeeded = chatService?.sendMessage(message.toByteArray())
        if (succeeded != true) {
            _errors.tryEmit("Sending message failed!")
            return null
        }

        return message
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    override fun release() {
        try {
            context.unregisterReceiver(foundDeviceReceiver)
            context.unregisterReceiver(pairingStatusReceiver)
        } catch (ex: IllegalArgumentException) {
            Log.d("BluetoothController", "some or all receivers are already unregistered!")
        }
        scanningCountdownTimer.cancel()
        closeConnection()
    }

    override fun updatePairedDevices() {
        checkBluetoothEnabledOr { return }
        checkConnectPermissionsGrantedOr { return }

        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toDomainDevice() }
            ?.also { updatedDevices -> _pairedDevices.update { updatedDevices } }
    }

    private inline fun checkBluetoothEnabledOr(action: () -> Unit) {
        if (bluetoothAdapter?.isEnabled != true) {
            _errors.tryEmit("Bluetooth is turned off! turn it on & try again.")
            _isEnabled.update { false }
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
        private const val CommunicationServiceUUID = "be5866ce-9dcf-4807-a65c-b3f7d8b3aebd"
        // Legacy permissions required for Android versions below S (API 31).
        private val LegacyPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
