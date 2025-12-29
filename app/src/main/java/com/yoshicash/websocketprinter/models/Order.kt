package com.yoshicash.websocketprinter.models

import java.io.Serializable

data class Order(
    val uuid: String = "",
    val saleCode: String = "",
    val sellerName: String = "",
    val concessionName: String = "",
    val products: List<Product> = emptyList(),
    val forcePrint: Int = 0
) : Serializable

data class Product(
    val productName : String = "",
    val quantity : Int = 0,
    val comment : List<String> = emptyList()
) : Serializable

