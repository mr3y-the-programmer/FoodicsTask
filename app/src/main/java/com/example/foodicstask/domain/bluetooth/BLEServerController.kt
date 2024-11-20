package com.example.foodicstask.domain.bluetooth

interface BLEServerController {

    suspend fun startServer()

    suspend fun stopServer()
}
