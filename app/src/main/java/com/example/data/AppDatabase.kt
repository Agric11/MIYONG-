package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ClientDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SettingDao
import com.example.data.entity.ClientEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SettingEntity

@Database(
    entities = [
        ClientEntity::class,
        ProductEntity::class,
        InvoiceEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "liyca_invoice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
