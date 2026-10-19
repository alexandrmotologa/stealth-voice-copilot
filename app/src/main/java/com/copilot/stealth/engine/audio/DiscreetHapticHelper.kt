package com.copilot.stealth.engine.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages covert haptic pulses to communicate system state through pocket vibration.
 */
class DiscreetHapticHelper(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Single subtle pulse: speech activity detected and recording started.
     */
    fun pulseSpeechDetected() {
        vibrate(timings = longArrayOf(0, 30), amplitudes = intArrayOf(0, 60))
    }

    /**
     * Double soft pulse: response generation complete and displayed on screen.
     */
    fun pulseAnswerReady() {
        vibrate(timings = longArrayOf(0, 40, 60, 40), amplitudes = intArrayOf(0, 80, 0, 80))
    }

    /**
     * Triple short pulse: dialogue history wiped via physical hardware button.
     */
    fun pulseContextCleared() {
        vibrate(
            timings = longArrayOf(0, 25, 40, 25, 40, 25),
            amplitudes = intArrayOf(0, 100, 0, 100, 0, 100)
        )
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings.sum())
        }
    }
}
