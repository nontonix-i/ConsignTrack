package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: TransactionHeader): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<TransactionDetail>)

    @Query("SELECT * FROM transaction_headers ORDER BY transaction_date DESC")
    fun getAllHeaders(): Flow<List<TransactionHeader>>

    @Query("SELECT * FROM transaction_headers ORDER BY transaction_date DESC")
    suspend fun getAllHeadersDirect(): List<TransactionHeader>

    @Query("SELECT * FROM transaction_headers WHERE customer_id = :customerId ORDER BY transaction_date DESC")
    fun getHeadersForCustomer(customerId: Long): Flow<List<TransactionHeader>>

    @Query("SELECT * FROM transaction_headers WHERE customer_id = :customerId ORDER BY transaction_date DESC LIMIT 1")
    fun getLatestHeaderForCustomer(customerId: Long): Flow<TransactionHeader?>

    @Query("SELECT * FROM transaction_headers WHERE customer_id = :customerId ORDER BY transaction_date DESC LIMIT 1")
    suspend fun getLatestHeaderForCustomerDirect(customerId: Long): TransactionHeader?

    @Query("SELECT * FROM transaction_headers WHERE transaction_date >= :startOfDay ORDER BY transaction_date DESC")
    fun getTodayTransactions(startOfDay: Long): Flow<List<TransactionHeader>>

    @Query("SELECT * FROM transaction_headers WHERE transaction_date >= :startOfDay ORDER BY transaction_date DESC")
    suspend fun getTodayTransactionsDirect(startOfDay: Long): List<TransactionHeader>

    @Query("SELECT * FROM transaction_details WHERE transaction_id = :headerId")
    fun getDetailsForHeader(headerId: Long): Flow<List<TransactionDetail>>

    @Query("SELECT * FROM transaction_details WHERE transaction_id = :headerId")
    suspend fun getDetailsForHeaderDirect(headerId: Long): List<TransactionDetail>

    @Query("SELECT * FROM transaction_details")
    suspend fun getAllDetailsDirect(): List<TransactionDetail>

    @Query("SELECT COALESCE(SUM(total_sold_amount), 0) FROM transaction_headers WHERE transaction_date >= :startOfDay")
    fun getTodayTotalSoldAmount(startOfDay: Long): Flow<Double>

    @Query("DELETE FROM transaction_details WHERE transaction_id = :headerId")
    suspend fun deleteDetailsForHeader(headerId: Long)

    @Query("DELETE FROM transaction_headers WHERE id = :headerId")
    suspend fun deleteHeaderById(headerId: Long)

    @Query("DELETE FROM transaction_details")
    suspend fun deleteAllDetails()

    @Query("DELETE FROM transaction_headers")
    suspend fun deleteAllHeaders()
}
