package com.example.util

import android.content.Context
import com.example.data.InvoiceRepository
import com.example.data.entity.ClientEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SettingEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

data class DatabaseBackup(
    val clients: List<ClientEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList()
)

object BackupRestoreManager {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(DatabaseBackup::class.java)

    suspend fun exportDatabaseBackup(repository: InvoiceRepository): String {
        val clients = repository.allClients.first()
        val products = repository.allProducts.first()
        val invoices = repository.allInvoices.first()
        val settings = repository.allSettings.first()

        val backup = DatabaseBackup(
            clients = clients,
            products = products,
            invoices = invoices,
            settings = settings
        )
        return adapter.toJson(backup)
    }

    suspend fun importDatabaseBackup(repository: InvoiceRepository, jsonString: String): Boolean {
        return try {
            val backup = adapter.fromJson(jsonString) ?: return false

            // Restore clients
            for (client in backup.clients) {
                repository.insertClient(client)
            }
            // Restore products
            for (product in backup.products) {
                repository.insertProduct(product)
            }
            // Restore invoices
            for (invoice in backup.invoices) {
                repository.insertInvoice(invoice)
            }
            // Restore settings
            repository.saveSettings(backup.settings)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
