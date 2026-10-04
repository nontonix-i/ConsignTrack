package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object FinancialCategory {
    const val BUSINESS_INCOME = "BUSINESS_INCOME"
    const val BUSINESS_EXPENSE = "BUSINESS_EXPENSE"
    const val PERSONAL_INCOME = "PERSONAL_INCOME"
    const val PERSONAL_EXPENSE = "PERSONAL_EXPENSE"
}

@Entity(tableName = "financial_records")
data class FinancialRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "category")
    val category: String, // BUSINESS_INCOME, BUSINESS_EXPENSE, PERSONAL_INCOME, PERSONAL_EXPENSE
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "transaction_date")
    val transaction_date: Long = System.currentTimeMillis()
)
