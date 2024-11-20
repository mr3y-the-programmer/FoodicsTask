package com.example.foodicstask.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodicstask.domain.bluetooth.BLEServerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BLEServerViewModel @Inject constructor(
    private val server: BLEServerController
) : ViewModel() {

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    fun startServer() {
        viewModelScope.launch {
            server.startServer()
            _isServerRunning.update { true }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            server.stopServer()
            _isServerRunning.update { false }
        }
    }
}
