package com.example.foodicstask.domain.model

data class Message(
    val text: String,
    val senderName: String,
    val isFromCurrentUser: Boolean
)
