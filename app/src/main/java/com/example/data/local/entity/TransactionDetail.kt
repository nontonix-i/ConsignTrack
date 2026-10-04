package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_details",
    foreignKeys = [
        ForeignKey(
            entity = TransactionHeader::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["product_id"])
    ]
)
data class TransactionDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "transaction_id")
    val transaction_id: Long,
    @ColumnInfo(name = "product_id")
    val product_id: Long,
    @ColumnInfo(name = "previous_stock")
    val previous_stock: Int,
    @ColumnInfo(name = "remaining_stock")
    val remaining_stock: Int,
    @ColumnInfo(name = "sold_quantity")
    val sold_quantity: Int,
    @ColumnInfo(name = "returned_quantity")
    val returned_quantity: Int = 0,
    @ColumnInfo(name = "added_quantity")
    val added_quantity: Int = 0,
    @ColumnInfo(name = "unit_price")
    val unit_price: Double
)
