package com.yoshicash.websocketprinter.print

import com.yoshicash.websocketprinter.models.Order

class PrintBuilder(val order: Order) {

    fun generateKitchenOrderTicket() : String {
        var pText = ""

        if (order.cash == true) {
            pText += "[C]<reverse><b>POR PAGAR EN EFECTIVO</b></reverse>\n"
            pText += "[L]\n"
        }
        pText += "[C]<u><font size='big'>Yoshicash</font></u>\n"
        pText += "[L]\n"
        pText +=  "[C]<u><font size='tall'>#${order.saleCode}</font></u>\n"
        pText += "[L]\n"
        pText += "[C]<u type='double'>Vendedor: ${order.sellerName}</u>"
        pText += "[L]\n"
        pText += "[L]\n"
        pText += "[C]<u type='double'>Concesion: ${order.concessionName}</u>"
        pText += "[L]\n"
        pText += "[C]================================\n"
        pText += "[L]\n"
        pText += "[L]<b>Productos</b>\n"
        pText += "[L]\n"

        for (product in order.products.listIterator()) {
            pText += "[L]<b>x${product.quantity} ${product.productName}</b>\n"

            for (comment in product.comment) {
                pText += "[L]$comment\n"
            }

            pText += "[L]\n"
        }

        pText += "[L]\n"
        pText += "[C]================================"
        pText += "[L] \n"
        pText += "[L] \n"
        pText += "[L] \n"

        return pText
    }

}