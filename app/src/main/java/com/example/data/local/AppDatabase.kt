package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConsignmentStockDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.FinancialRecordDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        Customer::class,
        Product::class,
        ConsignmentStock::class,
        TransactionHeader::class,
        TransactionDetail::class,
        FinancialRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun consignmentStockDao(): ConsignmentStockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun financialRecordDao(): FinancialRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE consignment_stocks ADD COLUMN custom_price_pack REAL DEFAULT NULL")
            }
        }

        /**
         * Pre-production migration (v5 -> v6):
         * Clears all initial mock/demo data from earlier development versions so the app starts clean.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transaction_details")
                db.execSQL("DELETE FROM transaction_headers")
                db.execSQL("DELETE FROM consignment_stocks")
                db.execSQL("DELETE FROM financial_records")
                db.execSQL("DELETE FROM customers")
                db.execSQL("DELETE FROM products")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consign_track.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
