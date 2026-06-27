package com.example.data.dao

import androidx.room.*
import com.example.data.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentInvoices(limit: Int): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Int): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM invoices WHERE isPaid = 0")
    fun getUnpaidCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices")
    suspend fun getAllInvoicesList(): List<InvoiceEntity>
}
