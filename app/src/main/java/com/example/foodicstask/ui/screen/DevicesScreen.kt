package com.example.foodicstask.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodicstask.domain.model.Device
import com.example.foodicstask.presentation.BluetoothUiState
import com.example.foodicstask.ui.components.BluetoothDeviceList
import java.text.DecimalFormat

@Composable
fun DevicesScreen(
    state: BluetoothUiState,
    isDeviceDiscoverable: Boolean,
    onStartScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onStartServerClick: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    onEnableDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "You're not connected to any device.",
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )
        BluetoothDeviceList(
            state.pairedDevices,
            state.scannedDevices,
            onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        ControlPanel(
            scanningRemainingSeconds = { state.scanningRemainingTime },
            isDeviceDiscoverable = isDeviceDiscoverable,
            onStartScanClick = onStartScanClick,
            onStopScanClick = onStopScanClick,
            onStartServerClick = onStartServerClick,
            onEnableDiscoverClick = onEnableDiscoverClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ControlPanel(
    scanningRemainingSeconds: () -> Int?,
    isDeviceDiscoverable: Boolean,
    onStartScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onStartServerClick: () -> Unit,
    onEnableDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScanningTimeout(
            scanningRemainingSeconds = scanningRemainingSeconds,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onStartScanClick) {
                Text("Start scan")
            }
            Button(onClick = onStopScanClick) {
                Text("Stop scan")
            }
            Button(onClick = onStartServerClick) {
                Text("Start Server")
            }
        }
        Text(
            text = if (isDeviceDiscoverable) "Your device is discoverable" else "Your device isn't discoverable",
        )
        Button(
            onClick = onEnableDiscoverClick,
            enabled = !isDeviceDiscoverable
        ) {
            Text("Enable discoverability")
        }
    }
}

@Composable
private fun ScanningTimeout(
    scanningRemainingSeconds: () -> Int?,
    modifier: Modifier = Modifier
) {
    val remainingSeconds = scanningRemainingSeconds()
    if (remainingSeconds != null) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
        ) {
            Text(
                text = "Scanning...",
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = DecimalFormat("00").format(remainingSeconds),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

