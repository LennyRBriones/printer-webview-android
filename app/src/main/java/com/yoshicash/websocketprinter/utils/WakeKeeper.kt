package com.yoshicash.websocketprinter.utils

import android.content.Context
import android.os.PowerManager

class WakeKeeper(ctx: Context) {
    private val pm = ctx.getSystemService(PowerManager::class.java)
    private var wake: PowerManager.WakeLock? = null
    fun acquire() {
        if (wake?.isHeld == true) return
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "app:ws").apply { acquire() }
    }
    fun release() { wake?.takeIf { it.isHeld }?.release() }
}