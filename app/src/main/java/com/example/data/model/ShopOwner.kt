package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_owners")
data class ShopOwner(
    @PrimaryKey val email: String,
    val passwordHash: String, // simple hashed / offline password
    val shopName: String,
    val location: String,
    val contactNumber: String,
    val currencySymbol: String = "$",
    val taxRate: Double = 0.0,
    val additionalNotes: String = "Thank you for your business!"
)
