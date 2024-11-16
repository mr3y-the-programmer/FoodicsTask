package com.example.foodicstask

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver that is triggered whenever the device's discoverability status changes.
 */
class DiscoverabilityStatusReceiver(
    private val onStatusChanged: (isDiscoverable: Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val newScanMode = intent?.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
        when (newScanMode) {
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> onStatusChanged(true)
            BluetoothAdapter.SCAN_MODE_NONE -> onStatusChanged(false)
            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> onStatusChanged(false)
        }
    }
}
