package com.example.data

import com.example.data.dao.ClientDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SettingDao
import com.example.data.entity.ClientEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    private val clientDao: ClientDao,
    private val productDao: ProductDao,
    private val invoiceDao: InvoiceDao,
    private val settingDao: SettingDao
) {
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()
    val recentInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getRecentInvoices(3)
    val unpaidCount: Flow<Int> = invoiceDao.getUnpaidCountFlow()

    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allSettings: Flow<List<SettingEntity>> = settingDao.getAllSettings()

    suspend fun getInvoiceById(id: Int): InvoiceEntity? {
        return invoiceDao.getInvoiceById(id)
    }

    suspend fun insertInvoice(invoice: InvoiceEntity): Long {
        return invoiceDao.insertInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        invoiceDao.deleteInvoice(invoice)
    }

    suspend fun getClientById(id: Int): ClientEntity? {
        return clientDao.getClientById(id)
    }

    suspend fun insertClient(client: ClientEntity): Long {
        return clientDao.insertClient(client)
    }

    suspend fun deleteClient(client: ClientEntity) {
        clientDao.deleteClient(client)
    }

    suspend fun getProductById(id: Int): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun getSetting(key: String): String? {
        return settingDao.getSettingByKey(key)
    }

    fun getSettingFlow(key: String): Flow<String?> {
        return settingDao.getSettingByKeyFlow(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(SettingEntity(key, value))
    }

    suspend fun saveSettings(settings: List<SettingEntity>) {
        settingDao.insertSettings(settings)
    }

    suspend fun getAllInvoicesList(): List<InvoiceEntity> {
        return invoiceDao.getAllInvoicesList()
    }
}
