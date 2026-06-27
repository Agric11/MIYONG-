package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InvoiceRepository
import com.example.data.entity.ClientEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItem
import com.example.data.entity.ProductEntity
import com.example.data.entity.SettingEntity
import com.example.util.BackupRestoreManager
import com.example.util.InvoicePdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class Screen {
    Dashboard,
    InvoiceBuilder,
    Settings,
    ClientManager,
    ProductManager
}

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InvoiceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = InvoiceRepository(
            clientDao = database.clientDao(),
            productDao = database.productDao(),
            invoiceDao = database.invoiceDao(),
            settingDao = database.settingDao()
        )
    }

    // Reactive database streams
    val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentInvoices: StateFlow<List<InvoiceEntity>> = repository.recentInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidCount: StateFlow<Int> = repository.unpaidCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allClients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSettings: StateFlow<List<SettingEntity>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    var currentScreen by mutableStateOf(Screen.Dashboard)
        private set

    // Selected invoice for editing (if null, we are building a new invoice)
    var editingInvoiceId by mutableStateOf<Int?>(null)
        private set

    // Form inputs for Invoice Builder
    var invoiceNumber by mutableStateOf("")
    var createdAt by mutableStateOf(System.currentTimeMillis())
    var dueDate by mutableStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) // 7 days default
    var businessName by mutableStateOf("")
    var businessAddress by mutableStateOf("")
    var businessLogoPath by mutableStateOf<String?>(null)
    var clientName by mutableStateOf("")
    var clientContact by mutableStateOf("")
    var paymentInstructions by mutableStateOf("")
    var companyStampPath by mutableStateOf<String?>(null)
    var signaturePath by mutableStateOf<String?>(null)
    var isPaid by mutableStateOf(false)
    val invoiceItems = mutableStateListOf<InvoiceItem>()

    // Live calculations
    val subtotal: Double
        get() = invoiceItems.sumOf { it.quantity * it.price }

    val totalDiscount: Double
        get() = invoiceItems.sumOf { (it.quantity * it.price) * (it.discountPercent / 100.0) }

    val totalTax: Double
        get() = invoiceItems.sumOf { item ->
            val itemSubtotal = item.quantity * item.price
            val itemDiscount = itemSubtotal * (item.discountPercent / 100.0)
            val itemNet = itemSubtotal - itemDiscount
            if (item.hasPpn) itemNet * 0.11 else 0.0
        }

    val totalBill: Double
        get() = subtotal - totalDiscount + totalTax

    fun navigateTo(screen: Screen) {
        currentScreen = screen
        if (screen == Screen.Dashboard) {
            clearBuilderForm()
        }
    }

    fun startNewInvoice() {
        clearBuilderForm()
        editingInvoiceId = null
        
        // Auto generate receipt number
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        invoiceNumber = "INV-$timestamp-$randomSuffix"

        // Load Defaults
        viewModelScope.launch {
            businessName = repository.getSetting("default_business_name") ?: "Liyca Business"
            businessAddress = repository.getSetting("default_business_address") ?: "Jakarta, Indonesia"
            businessLogoPath = repository.getSetting("default_business_logo")
            paymentInstructions = repository.getSetting("default_payment_method") ?: "Transfer Bank BCA: 12345678 a/n Liyca"
            companyStampPath = repository.getSetting("default_company_stamp")
        }

        navigateTo(Screen.InvoiceBuilder)
    }

    fun editInvoice(invoice: InvoiceEntity) {
        editingInvoiceId = invoice.id
        invoiceNumber = invoice.invoiceNumber
        createdAt = invoice.createdAt
        dueDate = invoice.dueDate
        businessName = invoice.businessName
        businessAddress = invoice.businessAddress
        businessLogoPath = invoice.businessLogoPath
        clientName = invoice.clientName
        clientContact = invoice.clientContact
        paymentInstructions = invoice.paymentInstructions
        companyStampPath = invoice.companyStampPath
        signaturePath = invoice.signaturePath
        isPaid = invoice.isPaid

        invoiceItems.clear()
        invoiceItems.addAll(InvoicePdfGenerator.parseItemsJson(invoice.itemsJson))

        navigateTo(Screen.InvoiceBuilder)
    }

    fun addInvoiceItem(item: InvoiceItem) {
        invoiceItems.add(item)
    }

    fun removeInvoiceItem(index: Int) {
        if (index in invoiceItems.indices) {
            invoiceItems.removeAt(index)
        }
    }

    fun updateInvoiceItem(index: Int, item: InvoiceItem) {
        if (index in invoiceItems.indices) {
            invoiceItems[index] = item
        }
    }

    fun saveInvoice() {
        viewModelScope.launch {
            val invoice = InvoiceEntity(
                id = editingInvoiceId ?: 0,
                invoiceNumber = invoiceNumber,
                createdAt = createdAt,
                dueDate = dueDate,
                businessName = businessName,
                businessAddress = businessAddress,
                businessLogoPath = businessLogoPath,
                clientName = clientName,
                clientContact = clientContact,
                paymentInstructions = paymentInstructions,
                companyStampPath = companyStampPath,
                signaturePath = signaturePath,
                isPaid = isPaid,
                itemsJson = InvoicePdfGenerator.itemsToJson(invoiceItems)
            )
            repository.insertInvoice(invoice)
            navigateTo(Screen.Dashboard)
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }

    private fun clearBuilderForm() {
        editingInvoiceId = null
        invoiceNumber = ""
        createdAt = System.currentTimeMillis()
        dueDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        businessName = ""
        businessAddress = ""
        businessLogoPath = null
        clientName = ""
        clientContact = ""
        paymentInstructions = ""
        companyStampPath = null
        signaturePath = null
        isPaid = false
        invoiceItems.clear()
    }

    // Client Management
    fun saveClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.insertClient(client)
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // Product Catalog Management
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Save Settings
    fun saveSettings(defaultBizName: String, defaultBizAddr: String, defaultPayment: String) {
        viewModelScope.launch {
            repository.saveSetting("default_business_name", defaultBizName)
            repository.saveSetting("default_business_address", defaultBizAddr)
            repository.saveSetting("default_payment_method", defaultPayment)
        }
    }

    // Helper to copy bitmap to app files directory for persistence
    fun saveBitmapToLocalFiles(bitmap: Bitmap, prefix: String): String? {
        return try {
            val context = getApplication<Application>()
            val directory = File(context.filesDir, "assets")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "$prefix-${UUID.randomUUID()}.png"
            val file = File(directory, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Local copy of external picked image URI for stability
    fun saveUriToLocalFiles(context: Context, uri: android.net.Uri, prefix: String): String? {
        return try {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                saveBitmapToLocalFiles(bitmap, prefix)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Export & Import Database
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupRestoreManager.exportDatabaseBackup(repository)
            onResult(json)
        }
    }

    fun importBackup(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupRestoreManager.importDatabaseBackup(repository, jsonString)
            onResult(success)
        }
    }

    // Render local PDF for sharing
    fun generateInvoicePdf(context: Context): android.graphics.pdf.PdfDocument {
        val invoice = InvoiceEntity(
            id = editingInvoiceId ?: 0,
            invoiceNumber = invoiceNumber,
            createdAt = createdAt,
            dueDate = dueDate,
            businessName = businessName,
            businessAddress = businessAddress,
            businessLogoPath = businessLogoPath,
            clientName = clientName,
            clientContact = clientContact,
            paymentInstructions = paymentInstructions,
            companyStampPath = companyStampPath,
            signaturePath = signaturePath,
            isPaid = isPaid,
            itemsJson = InvoicePdfGenerator.itemsToJson(invoiceItems)
        )
        return InvoicePdfGenerator.generateInvoicePdf(context, invoice)
    }
}
