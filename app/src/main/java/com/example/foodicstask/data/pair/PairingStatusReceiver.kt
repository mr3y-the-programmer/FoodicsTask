package com.example.foodicstask.data.pair

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Receives response whether pairing succeeded or failed.
 */
class PairingStatusReceiver(
    private val onStatusChanged: (isConnected: Boolean, BluetoothDevice) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                onStatusChanged(true, device ?: return)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                onStatusChanged(false, device ?: return)
            }
        }
    }
}