package com.example.data.repository

import com.example.data.dao.InvoiceDao
import com.example.data.dao.ShopOwnerDao
import com.example.data.model.Invoice
import com.example.data.model.ShopOwner
import kotlinx.coroutines.flow.Flow

class BillingRepository(
    private val shopOwnerDao: ShopOwnerDao,
    private val invoiceDao: InvoiceDao
) {
    suspend fun getOwnerByEmail(email: String): ShopOwner? {
        return shopOwnerDao.getOwnerByEmail(email)
    }

    fun getOwnerFlowByEmail(email: String): Flow<ShopOwner?> {
        return shopOwnerDao.getOwnerFlowByEmail(email)
    }

    suspend fun registerOwner(owner: ShopOwner) {
        shopOwnerDao.insertOwner(owner)
    }

    suspend fun updateOwner(owner: ShopOwner) {
        shopOwnerDao.updateOwner(owner)
    }

    fun getInvoicesForOwner(email: String): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesForOwner(email)
    }

    suspend fun createInvoice(invoice: Invoice): Long {
        return invoiceDao.insertInvoice(invoice)
    }

    suspend fun updateInvoiceStatus(invoiceId: Int, status: String) {
        invoiceDao.updateInvoiceStatus(invoiceId, status)
    }

    suspend fun deleteInvoice(invoiceId: Int) {
        invoiceDao.deleteInvoice(invoiceId)
    }

    suspend fun getInvoiceById(invoiceId: Int): Invoice? {
        return invoiceDao.getInvoiceById(invoiceId)
    }
}
