package com.yoshicash.websocketprinter.print

import com.yoshicash.websocketprinter.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrintBuilder(val order: Order) {

    private fun nowString(): String {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatMoney(value: Double?): String {
        if (value == null) return "--"
        return "$" + String.format(Locale.getDefault(), "%.2f", value)
    }

    fun generateKitchenOrderTicket(): String {
        var pText = ""

        pText += "[C]<b><font size='big'>Yoshicash</font></b>\n"
        pText += "[C]${nowString()}\n"

        if (order.concessionName.isNotBlank()) {
            pText += "[C]${order.concessionName}\n"
        }
        if (order.sellerName.isNotBlank()) {
            pText += "[C]<font size='big'>Vendedor: ${order.sellerName}</font>\n"
        }

        if (order.saleCode.isNotBlank()) {
            pText += "[C]<font size='big'>Tx:${order.saleCode}</font>\n"
        }

        pText += "[L]\n"
        pText += "[C]==================================\n"
        pText += "[L]\n"

        pText += "[L]<b>Consumo</b>\n"
        pText += "[L]\n"

        for (product in order.products.listIterator()) {
            val qty = if (product.quantity > 0) product.quantity else 1
            val name = product.productName
            if (name.isBlank()) continue
            val pad = if (product.isComplement) "   " else ""

            if (product.price != null) {
                val lineTotal = product.price * qty
                pText += "[L]${pad}x$qty $name[R]${formatMoney(lineTotal)}\n"
            } else {
                pText += "[L]${pad}x$qty $name\n"
            }

            for (comment in product.comment) {
                pText += "[L]$comment\n"
            }

            pText += "[L]\n"
        }

        val printablePayments = order.payments
            .filter { !it.paymentType.isNullOrBlank() && it.amount != null }

        if (printablePayments.isNotEmpty()) {
            pText += "[L]\n"
            pText += "[L]<b>Pagos</b>\n"
            pText += "[L]\n"

            val cashPending = order.cashAmount?.takeIf { it > 0 }

            for (p in printablePayments) {
                val rawType = p.paymentType!!.trim()
                val typeLower = rawType.lowercase(Locale.getDefault())

                val isCash = typeLower == "efectivo" || typeLower == "cash"
                val label = if (isCash && cashPending != null) {
                    "$rawType (<b>Por pagar</b>)"
                } else {
                    rawType
                }

                val amountToPrint = if (isCash && cashPending != null) cashPending else p.amount
                if (amountToPrint != null) {
                    pText += "[L]$label[R]${formatMoney(amountToPrint)}\n"
                }
            }
        }

        val total = order.payments.mapNotNull { it.amount }.sum().takeIf { it > 0.0 }
        if (total != null) {
            pText += "[L]\n"
            pText += "[C]----------------------------------\n"
            pText += "[R]<b>TOTAL: ${formatMoney(total)}</b>\n"
            pText += "[C]----------------------------------\n"
            pText += "[L]\n"
        }

        pText += "[L]\n[L]\n[L]\n"

        return pText
    }
}