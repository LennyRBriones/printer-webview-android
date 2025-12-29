package com.yoshicash.websocketprinter.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yoshicash.websocketprinter.ui.MainWebViewActivity

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(c: Context, i: Intent) {
        // ✅ Mantener tu comportamiento actual
        c.startActivity(Intent(c, MainWebViewActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        // ✅ Arrancar el servicio solo si no está ya ejecutándose
        createPrintChannel(c)
        PrintForegroundService.startIfNeeded(c)
    }


    @SuppressLint("ObsoleteSdkInt")
    fun createPrintChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "print_channel",
                "Servicio de impresión",
                NotificationManager.IMPORTANCE_LOW
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}