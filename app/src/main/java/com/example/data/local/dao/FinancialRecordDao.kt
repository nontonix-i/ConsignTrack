package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FinancialRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialRecordDao {
    @Query("SELECT * FROM financial_records ORDER BY transaction_date DESC")
    fun getAllRecords(): Flow<List<FinancialRecord>>

    @Query("SELECT * FROM financial_records ORDER BY transaction_date DESC")
    suspend fun getAllRecordsDirect(): List<FinancialRecord>

    @Query("SELECT * FROM financial_records WHERE category LIKE :prefix || '%' ORDER BY transaction_date DESC")
    fun getRecordsByPrefix(prefix: String): Flow<List<FinancialRecord>>

    @Query("SELECT * FROM financial_records WHERE category IN (:categories) ORDER BY transaction_date DESC")
    fun getRecordsByCategories(categories: List<String>): Flow<List<FinancialRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FinancialRecord): Long

    @Update
    suspend fun updateRecord(record: FinancialRecord)

    @Delete
    suspend fun deleteRecord(record: FinancialRecord)

    @Query("DELETE FROM financial_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM financial_records WHERE category LIKE :prefix || '%'")
    suspend fun deleteRecordsByPrefix(prefix: String)

    @Query("DELETE FROM financial_records")
    suspend fun deleteAllRecords()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM financial_records WHERE category = :category")
    fun getTotalByCategory(category: String): Flow<Double>
}
