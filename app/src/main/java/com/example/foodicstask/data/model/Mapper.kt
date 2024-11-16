package com.example.foodicstask.data.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.foodicstask.domain.model.Device
import com.example.foodicstask.domain.model.Message

@SuppressLint("MissingPermission")
internal fun BluetoothDevice.toDomainDevice(): Device {
    return Device(name, address)
}

fun Message.toByteArray(): ByteArray {
    return "$senderName^$text".encodeToByteArray()
}

fun String.toMessage(isFromCurrentUser: Boolean): Message {
    val (name, text) = split("^")
    return Message(text, name, isFromCurrentUser)
}
