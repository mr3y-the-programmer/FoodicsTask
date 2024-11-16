package com.example.foodicstask.data.chat

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.foodicstask.data.model.SessionCancelledException
import com.example.foodicstask.data.model.toMessage
import com.example.foodicstask.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class DataTransferService(
    private val socket: BluetoothSocket
) {
    fun listenForIncomingMessages(): Flow<Message> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val buffer = ByteArray(1024) // 1 KB buffer
            while (true) {
                val byteCount = try {
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    throw SessionCancelledException()
                }

                emit(buffer.decodeToString(endIndex = byteCount).toMessage(isFromCurrentUser = false))
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket.outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("DataTransferService", "Failed to send message, reason: $e")
                return@withContext false
            }

            true
        }
    }
}