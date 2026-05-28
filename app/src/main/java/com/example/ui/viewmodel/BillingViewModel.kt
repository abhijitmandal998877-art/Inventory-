package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.ShopOwner
import com.example.data.repository.BillingRepository
import com.example.util.PdfGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillingRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BillingRepository(database.shopOwnerDao(), database.invoiceDao())
    }

    // AUTH / ACCOUNT STATE
    private val _currentUser = MutableStateFlow<ShopOwner?>(null)
    val currentUser: StateFlow<ShopOwner?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    // Screen state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Auth)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // INVOICE BUILDER STATE
    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerContact = MutableStateFlow("")
    val customerContact: StateFlow<String> = _customerContact.asStateFlow()

    private val _activeCart = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val activeCart: StateFlow<List<InvoiceItem>> = _activeCart.asStateFlow()

    // Saved bills list
    @OptIn(ExperimentalCoroutinesApi::class)
    val invoicesList: StateFlow<List<Invoice>> = _currentUser
        .flatMapLatest { owner ->
            if (owner != null) {
                repository.getInvoicesForOwner(owner.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Subtotals
    val subtotal: StateFlow<Double> = _activeCart.map { items ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val taxAmount: StateFlow<Double> = combine(subtotal, _currentUser) { sub, user ->
        val rate = user?.taxRate ?: 0.0
        (sub * rate) / 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grandTotal: StateFlow<Double> = combine(subtotal, taxAmount) { sub, tax ->
        sub + tax
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // PDF sharing and export states
    private val _recentExportedPdf = MutableStateFlow<File?>(null)
    val recentExportedPdf: StateFlow<File?> = _recentExportedPdf.asStateFlow()

    private val _pdfGenerationMessage = MutableStateFlow<String?>(null)
    val pdfGenerationMessage: StateFlow<String?> = _pdfGenerationMessage.asStateFlow()

    // Seed preset products for Barcode lookup simulation
    val mockProducts = mapOf(
        "890101" to ProductPreset("Classic Milk Cafe", 2.50),
        "890202" to ProductPreset("Chocolate Cookie", 1.80),
        "890303" to ProductPreset("Hazelnut Donut", 3.20),
        "890404" to ProductPreset("Cold Brew Coffee", 4.50),
        "890505" to ProductPreset("Veggie Sandwich", 5.99),
        "999001" to ProductPreset("Wireless Mouse", 19.99),
        "999002" to ProductPreset("Mechanical Keyboard", 59.99),
        "999003" to ProductPreset("Full HD Web Camera", 34.50),
        "999004" to ProductPreset("Ergonomic Desk Mat", 15.00)
    )

    // Set screen path
    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    // Clear Auth error
    fun clearAuthError() {
        _authError.value = null
    }

    // SIGN UP Action
    fun registerOwner(
        email: String,
        passwordPlain: String,
        shopName: String,
        location: String,
        contactNumber: String,
        currencySymbol: String,
        taxRate: Double
    ) {
        if (email.isBlank() || passwordPlain.isBlank() || shopName.isBlank()) {
            _authError.value = "Email, Password and Shop Name are required!"
            return
        }

        viewModelScope.launch {
            try {
                val dbOwner = repository.getOwnerByEmail(email.trim().lowercase())
                if (dbOwner != null) {
                    _authError.value = "Owner email is already registered!"
                    return@launch
                }

                val newOwner = ShopOwner(
                    email = email.trim().lowercase(),
                    passwordHash = simpleHash(passwordPlain.trim()),
                    shopName = shopName.trim(),
                    location = location.trim(),
                    contactNumber = contactNumber.trim(),
                    currencySymbol = if (currencySymbol.trim().isEmpty()) "$" else currencySymbol.trim(),
                    taxRate = taxRate
                )

                repository.registerOwner(newOwner)
                _currentUser.value = newOwner
                _currentScreen.value = Screen.Billing
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = "Registration failed: ${e.localizedMessage}"
            }
        }
    }

    // LOGIN Action
    fun login(email: String, passwordPlain: String) {
        if (email.isBlank() || passwordPlain.isBlank()) {
            _authError.value = "Please complete all login credentials"
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            try {
                val owner = repository.getOwnerByEmail(email.trim().lowercase())
                if (owner == null) {
                    _authError.value = "No business owner found with this email"
                    _isLoggingIn.value = false
                    return@launch
                }

                val hash = simpleHash(passwordPlain.trim())
                if (owner.passwordHash == hash) {
                    _currentUser.value = owner
                    _currentScreen.value = Screen.Billing
                    _authError.value = null
                } else {
                    _authError.value = "Incorrect password! Please try again."
                }
            } catch (e: Exception) {
                _authError.value = "Login error: ${e.localizedMessage}"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    // UPDATE PROFILE SETTINGS
    fun updateProfile(
        shopName: String,
        location: String,
        contactNumber: String,
        currencySymbol: String,
        taxRate: Double,
        additionalNotes: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                shopName = shopName,
                location = location,
                contactNumber = contactNumber,
                currencySymbol = currencySymbol,
                taxRate = taxRate,
                additionalNotes = additionalNotes
            )
            repository.updateOwner(updated)
            _currentUser.value = updated
        }
    }

    // LOGOUT Action
    fun logout() {
        _currentUser.value = null
        clearCart()
        _currentScreen.value = Screen.Auth
    }

    // CART ACTIONS
    fun setCustomerDetails(name: String, contact: String) {
        _customerName.value = name
        _customerContact.value = contact
    }

    fun addItemToCart(name: String, price: Double, qty: Int) {
        if (name.isBlank() || price <= 0 || qty <= 0) return
        val current = _activeCart.value.toMutableList()
        val index = current.indexOfFirst { it.name.trim().lowercase() == name.trim().lowercase() }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + qty)
        } else {
            current.add(InvoiceItem(name.trim(), price, qty))
        }
        _activeCart.value = current
    }

    fun updateCartItemQuantity(index: Int, newQty: Int) {
        val current = _activeCart.value.toMutableList()
        if (index in current.indices) {
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            _activeCart.value = current
        }
    }

    fun removeCartItem(index: Int) {
        val current = _activeCart.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _activeCart.value = current
        }
    }

    fun clearCart() {
        _activeCart.value = emptyList()
        _customerName.value = ""
        _customerContact.value = ""
        _recentExportedPdf.value = null
    }

    // LOOKUP BARCODE
    fun scanBarcode(code: String): String? {
        val cleaned = code.trim()
        val preset = mockProducts[cleaned]
        return if (preset != null) {
            addItemToCart(preset.name, preset.price, 1)
            "Added: ${preset.name} - $${preset.price}"
        } else {
            null
        }
    }

    // SAVE INVOICE AND EXECUTE PDF GENERATION
    fun generateInvoice(paymentStatus: String, completion: (File) -> Unit = {}) {
        val owner = _currentUser.value ?: return
        val items = _activeCart.value
        val name = _customerName.value.ifBlank { "Walk-in Customer" }
        val contact = _customerContact.value.ifBlank { "N/A" }

        if (items.isEmpty()) {
            _pdfGenerationMessage.value = "Cart is empty! Add items first."
            return
        }

        viewModelScope.launch {
            try {
                // Generate consecutive invoice number: INV-YEAR-RAND
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val rand = (1000..9999).random()
                val invoiceNumber = "INV-$year-$rand"

                val sub = subtotal.value
                val tax = taxAmount.value
                val total = grandTotal.value

                val invoice = Invoice(
                    invoiceNumber = invoiceNumber,
                    ownerEmail = owner.email,
                    customerName = name,
                    customerContact = contact,
                    items = items,
                    subtotal = sub,
                    taxAmount = tax,
                    grandTotal = total,
                    paymentStatus = paymentStatus
                )

                // Save to local Room database
                repository.createInvoice(invoice)

                // Generate real PDF file
                val context = getApplication<Application>().applicationContext
                val pdfFile = PdfGenerator.generateInvoicePdf(context, invoice, owner)

                if (pdfFile != null) {
                    _recentExportedPdf.value = pdfFile
                    _pdfGenerationMessage.value = "Invoice PDF generated successfully!"
                    clearCart()
                    completion(pdfFile)
                } else {
                    _pdfGenerationMessage.value = "Failed to render PDF format."
                }
            } catch (e: Exception) {
                _pdfGenerationMessage.value = "Process error: ${e.localizedMessage}"
            }
        }
    }

    // UPDATE INVOICE DIRECTLY FROM LIST
    fun updateInvoicePaymentStatus(invoiceId: Int, status: String) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, status)
        }
    }

    // REGENERATE PDF FOR PREVIOUS BILL
    fun regeneratePreviousPdf(invoice: Invoice, context: Context, completion: (File) -> Unit) {
        val owner = _currentUser.value ?: return
        viewModelScope.launch {
            val pdfFile = PdfGenerator.generateInvoicePdf(context, invoice, owner)
            if (pdfFile != null) {
                completion(pdfFile)
            }
        }
    }

    // DELETE BILL RECORD
    fun deleteHistoricalInvoice(invoiceId: Int) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
        }
    }

    // Simple verification hashing for local database safety
    private fun simpleHash(plain: String): String {
        return plain.hashCode().toString(16) // basic hashed hex representation
    }

    data class ProductPreset(val name: String, val price: Double)
}

enum class Screen {
    Auth,
    Billing,
    Scanner,
    History,
    Profile
}
