package com.yoshicash.websocketprinter.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.yoshicash.websocketprinter.R
import com.yoshicash.websocketprinter.WebAndroidPrinterApplication
import com.yoshicash.websocketprinter.manager.PrintManagerInitCallback
import com.yoshicash.websocketprinter.manager.PrintManagerProcessCallback
import com.yoshicash.websocketprinter.models.Order

class PrintForegroundService : Service() {

    companion object {
        private val running: java.util.concurrent.atomic.AtomicBoolean =
            java.util.concurrent.atomic.AtomicBoolean(false)

        fun isRunning(): Boolean = running.get()

        @SuppressLint("ObsoleteSdkInt")
        fun startIfNeeded(context: Context) {
            if (running.get()) return
            val intent = Intent(context, PrintForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var isPrinting: Boolean = false

    // --------- Loop basado en función (evita autoreferencias del Runnable) ---------

    private fun scheduleNext(delayMs: Long) {
        handler.postDelayed({ tick() }, delayMs)
    }

    private fun tick() {
        if (isPrinting) {
            scheduleNext(1500)
            return
        }

        val sp = getSharedPreferences("kitchen_orders", MODE_PRIVATE)
        val set: Set<String> = sp.getStringSet("orders", emptySet()) ?: emptySet()
        val queue: List<String> = ArrayList(set) // copia segura
        log("Cola actual: size=${queue.size}")

        if (queue.isEmpty()) {
            scheduleNext(2000)
            return
        }

        val raw: String = queue.first()
        val order: Order = try {
            Gson().fromJson(raw, Order::class.java)
        } catch (e: Exception) {
            log("Error parseando Order: ${e.message}. Removiendo item corrupto.")
            val newSet: MutableSet<String> = set.toMutableSet().apply { remove(raw) }
            sp.edit().putStringSet("orders", newSet).apply()
            scheduleNext(500)
            return
        }

        if (findIfOrderIsAlreadyPrinted(order.uuid) && order.forcePrint == 0) {
            log("Pedido ya fue impreso. Removiendo item de la cola.")
            val newSet: MutableSet<String> = set.toMutableSet().apply { remove(raw) }
            sp.edit().putStringSet("orders", newSet).apply()
            scheduleNext(500)
            return
        }

        val printManager = (applicationContext as WebAndroidPrinterApplication).printManager

        isPrinting = true
        log("Inicializando impresora...")

        printManager.init(object : PrintManagerInitCallback {
            override fun onPrinterReady() {
                log("Impresora lista. Enviando pedido a imprimir...")
                printManager.print(order, object : PrintManagerProcessCallback {
                    override fun onPrintSuccess() {
                        log("Impresión exitosa. Eliminando pedido de la cola.")
                        val current: MutableSet<String> =
                            sp.getStringSet("orders", emptySet())?.toMutableSet() ?: mutableSetOf()
                        current.remove(raw)
                        sp.edit().putStringSet("orders", current).apply()

                        isPrinting = false
                        scheduleNext(500) // sigue con el siguiente rápido
                        addOrderUUIDToHistoricalPrint(order.uuid)
                    }

                    override fun onPrintError(message: String) {
                        log("Error de impresión: $message")
                        // Opcional: aquí puedes implementar backoff o reintentos
                        isPrinting = false
                        scheduleNext(2000)
                    }
                })
            }
        })
    }

    // ------------------------- Ciclo de vida del Service -------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running.compareAndSet(false, true)) {
            startForeground(1, createNotification())
            return START_STICKY
        }
        startForeground(1, createNotification())
        log("Servicio iniciado")
        // Arranca el loop
        scheduleNext(0)
        return START_STICKY
    }

    override fun onDestroy() {
        log("Servicio destruido")
        handler.removeCallbacksAndMessages(null)
        isPrinting = false
        running.set(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------ Utilidades ------------------------------

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "print_channel")
            .setContentTitle("Print Service")
            .setContentText("Buscando pedidos para imprimir...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun log(message: String) {
        Log.d("PrintService", message)
    }

    private fun addOrderUUIDToHistoricalPrint(uuid: String) {
        if (findIfOrderIsAlreadyPrinted(uuid)) return

        val sp = getSharedPreferences("historical_prints", MODE_PRIVATE)
        val current: Set<String> = sp.getStringSet("uuids", emptySet()) ?: emptySet()

        val newSet: MutableSet<String> = current.toMutableSet()
        newSet.add(uuid)
        sp.edit().putStringSet("uuids", newSet).apply()
        log("UUID agregado a historial: $uuid")
    }

    private fun findIfOrderIsAlreadyPrinted(uuid: String): Boolean  {
        val sp = getSharedPreferences("historical_prints", MODE_PRIVATE)
        val current: Set<String> = sp.getStringSet("uuids", emptySet()) ?: emptySet()
        return current.contains(uuid)
    }
}
