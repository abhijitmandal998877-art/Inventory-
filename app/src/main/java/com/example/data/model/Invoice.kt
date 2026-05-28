package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val ownerEmail: String,
    val customerName: String,
    val customerContact: String,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val paymentStatus: String // "PAID", "PENDING", "REFUNDED"
)
