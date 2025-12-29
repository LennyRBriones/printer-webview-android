package com.yoshicash.websocketprinter.manager

import android.os.Handler
import android.os.Looper
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.yoshicash.websocketprinter.models.Order
import com.yoshicash.websocketprinter.print.PrintBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PrintManager {

    private lateinit var mBluePrinter: EscPosPrinter
    private val printScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())

    private var mResultCallback: PrintManagerProcessCallback? = null

    fun init(callback: PrintManagerInitCallback) {
        mBluePrinter = EscPosPrinter(
            BluetoothPrintersConnections.selectFirstPaired(),
            203,
            48f,
            32,
            EscPosCharsetEncoding("windows-1252", 16)
        )

        handler.postDelayed({
            callback.onPrinterReady()
        }, 1000)
    }

    fun print(order: Order, callback: PrintManagerProcessCallback) {
        mResultCallback = callback

        printScope.launch {
            val ticketInfoStr = PrintBuilder(order).generateKitchenOrderTicket()
            mBluePrinter.printFormattedTextAndCut(ticketInfoStr, 10)

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                mBluePrinter.disconnectPrinter()
                mResultCallback?.onPrintSuccess()
            }, CONST_TIME_SECONDS_WAIT_ML)
        }
    }

    companion object {
        private const val CONST_TIME_SECONDS_WAIT_ML = 2500L
    }
}

interface PrintManagerInitCallback {
    fun onPrinterReady()
}

interface PrintManagerProcessCallback {
    fun onPrintSuccess()
    fun onPrintError(message: String)
}