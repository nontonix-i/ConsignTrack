package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConsignmentStockDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.FinancialRecordDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        Product::class,
        ConsignmentStock::class,
        TransactionHeader::class,
        TransactionDetail::class,
        FinancialRecord::class
    ],
    version = 4,
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

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consign_track.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseSeedCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeedCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val customerDao = db.customerDao()
            val productDao = db.productDao()
            val stockDao = db.consignmentStockDao()
            val financialDao = db.financialRecordDao()

            // Products with Satuan Besar (Pack) & Satuan Kecil (Pcs)
            val p1Id = productDao.insertProduct(
                Product(
                    name = "Kerupuk Kaleng Putih",
                    unit = "Pcs",
                    unit_small = "Pcs",
                    unit_big = "Pack",
                    pieces_per_pack = 10,
                    selling_price_pack = 16000.0,
                    cost_price_pack = 11500.0,
                    selling_price = 1600.0,
                    cost_price = 1150.0
                )
            )
            val p2Id = productDao.insertProduct(
                Product(
                    name = "Kerupuk Bawang Renyah",
                    unit = "Pcs",
                    unit_small = "Pcs",
                    unit_big = "Pack",
                    pieces_per_pack = 10,
                    selling_price_pack = 20000.0,
                    cost_price_pack = 14000.0,
                    selling_price = 2000.0,
                    cost_price = 1400.0
                )
            )
            val p3Id = productDao.insertProduct(
                Product(
                    name = "Makaroni Pedas Daun Jeruk",
                    unit = "Pcs",
                    unit_small = "Pcs",
                    unit_big = "Pack",
                    pieces_per_pack = 10,
                    selling_price_pack = 35000.0,
                    cost_price_pack = 24000.0,
                    selling_price = 3500.0,
                    cost_price = 2400.0
                )
            )
            val p4Id = productDao.insertProduct(
                Product(
                    name = "Kacang Bawang Super",
                    unit = "Pcs",
                    unit_small = "Pcs",
                    unit_big = "Bal",
                    pieces_per_pack = 10,
                    selling_price_pack = 45000.0,
                    cost_price_pack = 32000.0,
                    selling_price = 4500.0,
                    cost_price = 3200.0
                )
            )

            // Seed customers across different day routes
            // RUTE SENIN (Area Pasar Minggu - Cilandak)
            val c1 = customerDao.insertCustomer(
                Customer(name = "Warung Bu Sri", address = "Jl. Merdeka No. 12, Pasar Minggu", phone = "0812-3456-7890", route_day = "Senin", route_order = 1, latitude = -6.2845, longitude = 106.8432)
            )
            val c2 = customerDao.insertCustomer(
                Customer(name = "Toko Berkah Jaya", address = "Jl. Cempaka Putih No. 45", phone = "0813-9876-5432", route_day = "Senin", route_order = 2, latitude = -6.2891, longitude = 106.8488)
            )
            val c3 = customerDao.insertCustomer(
                Customer(name = "Kelontong Bu Dewi", address = "Jl. Pejaten Barat No. 88", phone = "0858-9900-1122", route_day = "Senin", route_order = 3, latitude = -6.2750, longitude = 106.8321)
            )

            // RUTE SELASA (Area Raya Bogor - Kramat Jati)
            val c4 = customerDao.insertCustomer(
                Customer(name = "Kelontong Pak Joko", address = "Jl. Raya Bogor KM 28", phone = "0857-1122-3344", route_day = "Selasa", route_order = 1, latitude = -6.2712, longitude = 106.8654)
            )
            val c5 = customerDao.insertCustomer(
                Customer(name = "Warung Sumber Rejeki", address = "Gang Kancil No. 8", phone = "0821-4455-6677", route_day = "Selasa", route_order = 2, latitude = -6.2680, longitude = 106.8710)
            )
            val c6 = customerDao.insertCustomer(
                Customer(name = "Toko Snack Sejahtera", address = "Jl. H. Bokir No. 19", phone = "0819-3322-1100", route_day = "Selasa", route_order = 3, latitude = -6.2910, longitude = 106.8755)
            )

            // RUTE RABU (Area Kebayoran - Gandaria)
            val c7 = customerDao.insertCustomer(
                Customer(name = "Warung Madura Barokah", address = "Jl. Kebayoran Lama No. 3", phone = "0877-6655-4433", route_day = "Rabu", route_order = 1, latitude = -6.2420, longitude = 106.7820)
            )
            val c8 = customerDao.insertCustomer(
                Customer(name = "Toko Sinar Terang", address = "Jl. Cipete Raya No. 22", phone = "0811-2233-4455", route_day = "Rabu", route_order = 2, latitude = -6.2780, longitude = 106.8040)
            )

            // RUTE KAMIS (Area Fatmawati - Pondok Labu)
            val c9 = customerDao.insertCustomer(
                Customer(name = "Kelontong Sentosa", address = "Jl. RS Fatmawati No. 50", phone = "0812-7788-9900", route_day = "Kamis", route_order = 1, latitude = -6.2995, longitude = 106.7960)
            )
            val c10 = customerDao.insertCustomer(
                Customer(name = "Warung Pojok Rejeki", address = "Jl. Pangkalan Jati No. 7", phone = "0852-3344-5566", route_day = "Kamis", route_order = 2, latitude = -6.3150, longitude = 106.7910)
            )

            // RUTE JUMAT (Area Tebet - Manggarai)
            val c11 = customerDao.insertCustomer(
                Customer(name = "Toko Beras & Snack Bu Ani", address = "Jl. Tebet Timur Dalam No. 14", phone = "0813-1122-3399", route_day = "Jumat", route_order = 1, latitude = -6.2350, longitude = 106.8520)
            )
            val c12 = customerDao.insertCustomer(
                Customer(name = "Warung Guyub Rukun", address = "Jl. Bukit Duri No. 5", phone = "0878-4455-6677", route_day = "Jumat", route_order = 2, latitude = -6.2210, longitude = 106.8590)
            )

            // RUTE SABTU (Area Pasar Rebo - Ciracas)
            val c13 = customerDao.insertCustomer(
                Customer(name = "Grosir Cemilan Makmur", address = "Jl. Raya Ciracas No. 101", phone = "0812-8899-0011", route_day = "Sabtu", route_order = 1, latitude = -6.3260, longitude = 106.8720)
            )

            // RUTE MINGGU (Area Ragunan - Kebagusan)
            val c14 = customerDao.insertCustomer(
                Customer(name = "Warung Bu RT Ragunan", address = "Jl. Taman Margasatwa No. 10", phone = "0812-7766-5544", route_day = "Minggu", route_order = 1, latitude = -6.3050, longitude = 106.8210)
            )
            val c15 = customerDao.insertCustomer(
                Customer(name = "Toko Snack Hari Minggu", address = "Jl. Kebagusan Raya No. 4", phone = "0856-1199-8877", route_day = "Minggu", route_order = 2, latitude = -6.3120, longitude = 106.8340)
            )

            // Seed initial consignment stock in active warungs
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c1, product_id = p1Id, current_quantity = 30))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c1, product_id = p2Id, current_quantity = 25))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c1, product_id = p3Id, current_quantity = 15))

            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c2, product_id = p1Id, current_quantity = 40))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c2, product_id = p2Id, current_quantity = 30))

            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c4, product_id = p1Id, current_quantity = 25))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c4, product_id = p3Id, current_quantity = 20))

            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c5, product_id = p2Id, current_quantity = 35))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c5, product_id = p4Id, current_quantity = 15))

            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c9, product_id = p1Id, current_quantity = 30))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c11, product_id = p2Id, current_quantity = 25))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c14, product_id = p1Id, current_quantity = 25))
            stockDao.insertOrUpdateStock(ConsignmentStock(customer_id = c15, product_id = p3Id, current_quantity = 20))

            // Seed sample operational cash records
            financialDao.insertRecord(
                FinancialRecord(
                    category = FinancialCategory.BUSINESS_EXPENSE,
                    amount = 35000.0,
                    description = "Bensin motor keliling rute",
                    transaction_date = System.currentTimeMillis() - 86400000L
                )
            )
            financialDao.insertRecord(
                FinancialRecord(
                    category = FinancialCategory.BUSINESS_EXPENSE,
                    amount = 20000.0,
                    description = "Plastik kemasan & tali rafia",
                    transaction_date = System.currentTimeMillis() - 86400000L
                )
            )
            financialDao.insertRecord(
                FinancialRecord(
                    category = FinancialCategory.PERSONAL_EXPENSE,
                    amount = 25000.0,
                    description = "Makan siang pribadi",
                    transaction_date = System.currentTimeMillis() - 43200000L
                )
            )
        }
    }
}
