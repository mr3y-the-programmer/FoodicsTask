package com.example.foodicstask

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodicstask.presentation.BluetoothViewModel
import com.example.foodicstask.ui.components.BluetoothNotSupportedView
import com.example.foodicstask.ui.screen.ChatScreen
import com.example.foodicstask.ui.screen.DevicesScreen
import com.example.foodicstask.ui.theme.FoodicsTaskTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        setContent {
            FoodicsTaskTheme {
                val viewModel = viewModel<BluetoothViewModel>()
                val state by viewModel.state.collectAsStateWithLifecycle()

                // Show a dialog to user explaining app cannot function without bluetooth feature & finish app on acknowledgment.
                if (bluetoothAdapter == null) {
                    BluetoothNotSupportedView(
                        onConfirmButtonClick = { finish() }
                    )
                }

                AskForRequiredPermissions(
                    isBluetoothInitiallyEnabled = state.isBluetoothEnabled,
                    onBluetoothEnabled = viewModel::updatePairedDevices
                )

                // Update paired devices list whenever app comes to foreground.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.updatePairedDevices()
                }

                var isDeviceDiscoverable by remember {
                    mutableStateOf(
                        bluetoothAdapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                    )
                }

                val context = LocalContext.current
                DisposableEffect(context) {
                    val receiver = DiscoverabilityStatusReceiver { isDiscoverable ->
                        isDeviceDiscoverable = isDiscoverable
                    }
                    context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED))

                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                val snackBarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.errors.collect { errorMessage ->
                        snackBarHostState.showSnackbar(errorMessage)
                    }
                }

                LaunchedEffect(state.isConnected) {
                    if (state.isConnected) {
                        snackBarHostState.showSnackbar("You're connected!")
                        viewModel.updatePairedDevices()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackBarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { contentPadding ->
                    when {
                        state.isConnecting -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(contentPadding)
                                    .fillMaxSize()
                            ) {
                                CircularProgressIndicator()
                                Text("Connecting...")
                            }
                        }
                        state.isConnected -> {
                            ChatScreen(
                                state = state,
                                onDisconnect = viewModel::disconnectFromDevice,
                                onSendMessage = viewModel::sendMessage,
                                modifier = Modifier
                                    .padding(contentPadding)
                                    .fillMaxSize()
                            )
                        }
                        else -> {
                            val enableDiscoverabilityLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) {}
                            DevicesScreen(
                                state = state,
                                isDeviceDiscoverable = isDeviceDiscoverable,
                                onStartScanClick = viewModel::startScan,
                                onStopScanClick = viewModel::stopScan,
                                onStartServerClick = viewModel::waitForIncomingConnections,
                                onDeviceClick = viewModel::connectToDevice,
                                onEnableDiscoverClick = {
                                    enableDiscoverabilityLauncher.launch(
                                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180) // 3 minutes
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .padding(contentPadding)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AskForRequiredPermissions(
        isBluetoothInitiallyEnabled: Boolean,
        onBluetoothEnabled: () -> Unit
    ) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val enableBluetoothLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                onBluetoothEnabled()
            }
        }

        val requestPermissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val canEnableBluetooth = permissions.values.all { it }

            if (canEnableBluetooth && !isBluetoothInitiallyEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        LaunchedEffect(Unit) {
            requestPermissionsLauncher.launch(requiredPermissions)
        }
    }
}
