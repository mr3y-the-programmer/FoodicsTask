package com.example.foodicstask.data.scan

import android.os.CountDownTimer

/**
 * 12 seconds timeout for device discovery operation to avoid draining battery if we kept running device discovery indefinitely.
 */
class ScanningCountdownTimer(
    private val onTickInterval: (remainingSeconds: Int) -> Unit,
    private val onStop: () -> Unit,
) : CountDownTimer(12_000L, 1000L) {

    override fun onTick(millisUntilFinished: Long) {
        onTickInterval((millisUntilFinished / 1000).toInt())
    }

    override fun onFinish() {
        onStop()
    }
}
