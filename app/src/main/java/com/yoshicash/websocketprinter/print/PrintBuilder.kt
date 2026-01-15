package com.yoshicash.websocketprinter.print

import com.yoshicash.websocketprinter.models.Order

class PrintBuilder(val order: Order) {

    fun generateKitchenOrderTicket(): String {
        var pText = ""

        val amount = order.cashAmount
        if (amount != null && amount > 0) {
            pText += "[C]<reverse><b>POR PAGAR $$amount EFECTIVO</b></reverse>\n"
            pText += "[L]\n"
        }

        pText += "[C]<b>Yoshicash</b>\n"

        val printedAt = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        pText += "[C]$printedAt\n"

        if (order.concessionName.isNotBlank()) pText += "[C]${order.concessionName}\n"
        if (order.sellerName.isNotBlank()) pText += "[C]Vendedor: ${order.sellerName}\n"

        if (amount != null && amount > 0) {
            pText += "[C]Pago en efectivo\n"
        }

        if (order.saleCode.isNotBlank()) {
            pText += "[C]Tx: ${order.saleCode}\n"
        }

        pText += "[L]\n"

        pText += "[L]<b>Consumo</b>\n"

        for (product in order.products.listIterator()) {
            val qty = product.quantity
            val name = product.productName
            pText += "[L]x$qty $name\n"

            for (comment in product.comment) {
                if (comment.isNotBlank()) pText += "[L]$comment\n"
            }

            pText += "[L]\n"
        }

        pText += "[L] \n[L] \n[L] \n"

        return pText
    }

}