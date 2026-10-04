package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "consignment_stocks",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
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
        Index(value = ["customer_id", "product_id"], unique = true),
        Index(value = ["customer_id"]),
        Index(value = ["product_id"])
    ]
)
data class ConsignmentStock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_id")
    val customer_id: Long,
    @ColumnInfo(name = "product_id")
    val product_id: Long,
    @ColumnInfo(name = "current_quantity")
    val current_quantity: Int,
    @ColumnInfo(name = "custom_price_pack")
    val custom_price_pack: Double? = null,
    @ColumnInfo(name = "last_updated")
    val last_updated: Long = System.currentTimeMillis()
) {
    fun effectivePricePack(product: Product): Double =
        if (custom_price_pack != null && custom_price_pack > 0.0) custom_price_pack else product.selling_price_pack

    fun effectivePriceUnit(product: Product): Double =
        if (custom_price_pack != null && custom_price_pack > 0.0) {
            if (product.pieces_per_pack > 0) custom_price_pack / product.pieces_per_pack else custom_price_pack
        } else {
            product.selling_price
        }

    fun effectivePriceSmall(product: Product): Double = effectivePriceUnit(product)
}
