package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConsignmentStock
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsignmentStockDao {
    @Query("SELECT * FROM consignment_stocks WHERE customer_id = :customerId")
    fun getStocksForCustomer(customerId: Long): Flow<List<ConsignmentStock>>

    @Query("SELECT * FROM consignment_stocks WHERE customer_id = :customerId")
    suspend fun getStocksForCustomerDirect(customerId: Long): List<ConsignmentStock>

    @Query("SELECT * FROM consignment_stocks WHERE customer_id = :customerId AND product_id = :productId LIMIT 1")
    suspend fun getStock(customerId: Long, productId: Long): ConsignmentStock?

    @Query("SELECT * FROM consignment_stocks")
    fun getAllStocks(): Flow<List<ConsignmentStock>>

    @Query("SELECT * FROM consignment_stocks")
    suspend fun getAllStocksDirect(): List<ConsignmentStock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStock(stock: ConsignmentStock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStocks(stocks: List<ConsignmentStock>)

    @Update
    suspend fun updateStock(stock: ConsignmentStock)

    @Query("SELECT COALESCE(SUM(current_quantity), 0) FROM consignment_stocks")
    fun getTotalConsignedPieces(): Flow<Int>

    @Query("DELETE FROM consignment_stocks WHERE customer_id = :customerId")
    suspend fun deleteStocksForCustomer(customerId: Long)

    @Query("DELETE FROM consignment_stocks WHERE customer_id = :customerId AND product_id = :productId")
    suspend fun deleteStock(customerId: Long, productId: Long)
}
