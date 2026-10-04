package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_headers",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"])
    ]
)
data class TransactionHeader(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_id")
    val customer_id: Long,
    @ColumnInfo(name = "transaction_date")
    val transaction_date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "total_sold_amount")
    val total_sold_amount: Double,
    @ColumnInfo(name = "amount_paid")
    val amount_paid: Double,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "photo_uri")
    val photo_uri: String? = null
)
