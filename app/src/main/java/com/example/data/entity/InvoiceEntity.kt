package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

data class InvoiceItem(
    val name: String,
    val description: String,
    val quantity: Double,
    val price: Double,
    val discountPercent: Double,
    val hasPpn: Boolean
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val createdAt: Long,
    val dueDate: Long,
    val businessName: String,
    val businessAddress: String,
    val businessLogoPath: String? = null,
    val clientName: String,
    val clientContact: String,
    val paymentInstructions: String,
    val companyStampPath: String? = null,
    val signaturePath: String? = null,
    val isPaid: Boolean = false,
    val itemsJson: String // Moshi JSON list of InvoiceItem
)
