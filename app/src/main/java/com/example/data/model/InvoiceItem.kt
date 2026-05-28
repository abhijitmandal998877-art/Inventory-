package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InvoiceItem(
    val name: String,
    val price: Double,
    val quantity: Int
) {
    val totalPrice: Double get() = price * quantity
}
