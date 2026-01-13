package com.yoshicash.websocketprinter.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Order(
    val uuid: String = "",
    val saleCode: String = "",
    val sellerName: String = "",
    val concessionName: String = "",
    val products: List<Product> = emptyList(),
    val forcePrint: Int = 0,
    @SerializedName("cash_amount")
    val cashAmount: Double? = null
) : Serializable

data class Product(
    val productName : String = "",
    val quantity : Int = 0,
    val comment : List<String> = emptyList()
) : Serializable
