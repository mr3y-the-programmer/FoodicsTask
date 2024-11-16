package com.example.foodicstask.domain.model

sealed interface ConnectionResult {
    data object ConnectionEstablished : ConnectionResult
    data class TransferSucceeded(val message: Message) : ConnectionResult
    data object Error : ConnectionResult
}
