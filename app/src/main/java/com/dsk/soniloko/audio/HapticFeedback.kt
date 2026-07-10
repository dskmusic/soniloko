package com.dsk.soniloko.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short tap vibration. Uses the Vibrator service directly (not View.performHapticFeedback,
 * which some OEMs gate behind a "touch feedback" system toggle) so it reliably fires regardless
 * of ringer mode — silent/vibrate mode doesn't suppress app-triggered vibration, only ringtone
 * and notification sound/vibration do. (A hard "Total silence" DND override, where it exists, is
 * an OS-level block nothing can bypass.)
 */
object HapticFeedback {
    private const val TAP_DURATION_MS = 15L

    fun tap(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(TAP_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
