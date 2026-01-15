package com.yoshicash.websocketprinter.interfaces

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.yoshicash.websocketprinter.models.Order
import com.yoshicash.websocketprinter.service.PrintForegroundService

class AndroidBridge(
    private val context: Context,
    private val gson: Gson = Gson()
) {

    companion object {
        const val TAG = "AndroidBridge"
    }

    @JavascriptInterface
    fun printBl(strOrderObj: String) {
        log("printBl input: $strOrderObj")
        val order = parseOrder(strOrderObj)
            ?: run {
                Log.e(TAG, "JSON inválido de Order. No se imprime.")
                return
            }
        addOrderToQueue(order)
    }

    @JavascriptInterface
    fun cleanSharedPreferenceKitchenOrders() {
        val sp = context.getSharedPreferences("kitchen_orders", MODE_PRIVATE)
        sp.edit().clear().apply()

        val sp2 = context.getSharedPreferences("historical_prints", MODE_PRIVATE)
        sp2.edit().clear().apply()
    }

    private fun addOrderToQueue(order: Order) {
        val sp = context.getSharedPreferences("kitchen_orders", MODE_PRIVATE)
        val current: Set<String> = sp.getStringSet("orders", emptySet()) ?: emptySet()
        val orderJson: String = gson.toJson(order)

        val newSet: MutableSet<String> = current.toMutableSet()

        val added: Boolean = newSet.add(orderJson)
        if (added) {
            val ok = sp.edit().putStringSet("orders", newSet).commit() // commit para confirmar
            log("Nueva orden agregada a la cola (commit=$ok). size=${newSet.size}")

            PrintForegroundService.startIfNeeded(context)

            val verify = sp.getStringSet("orders", emptySet()) ?: emptySet()
            log("Verificación SP -> size=${verify.size}")
        } else {
            log("Orden ya existía. size=${newSet.size}")
        }
    }

    private fun orderExists(order: Order, pendingOrders: Set<String>): Boolean {
        val orderJson = gson.toJson(order)
        return pendingOrders.contains(orderJson)
    }

    // --- Helpers ---

    private fun parseOrder(json: String): Order? =
        runCatching { gson.fromJson(json, Order::class.java) }
            .onFailure { Log.e(TAG, "Error parseando Order desde JSON", it) }
            .getOrNull()

    private fun log(msg: String) = Log.d(TAG, msg)
}