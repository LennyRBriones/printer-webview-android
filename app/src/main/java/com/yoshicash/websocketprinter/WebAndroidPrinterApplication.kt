package com.yoshicash.websocketprinter

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yoshicash.websocketprinter.manager.PrintManager
import com.yoshicash.websocketprinter.service.PrintForegroundService

class WebAndroidPrinterApplication : Application() {

    val printManager: PrintManager by lazy {
        PrintManager()
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()

        createPrintChannel(this)
        val serviceIntent = Intent(this, PrintForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun createPrintChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "print_channel",
                "Servicio de impresión",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}