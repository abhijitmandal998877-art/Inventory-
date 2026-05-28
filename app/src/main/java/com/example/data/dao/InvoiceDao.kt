package com.example.data.dao

import androidx.room.*
import com.example.data.model.Invoice
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE ownerEmail = :ownerEmail ORDER BY timestamp DESC")
    fun getInvoicesForOwner(ownerEmail: String): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("UPDATE invoices SET paymentStatus = :status WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: Int, status: String)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteInvoice(invoiceId: Int)

    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceById(invoiceId: Int): Invoice?
}
