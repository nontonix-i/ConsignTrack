package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String = "Pcs",
    @ColumnInfo(name = "unit_small")
    val unit_small: String = "Pcs", // Satuan kecil: Pcs, Bungkus, Butir
    @ColumnInfo(name = "unit_big")
    val unit_big: String = "Pack", // Satuan besar: Pack, Bal, Kaleng, Dus
    @ColumnInfo(name = "pieces_per_pack")
    val pieces_per_pack: Int = 10, // Isi per pack (misal 1 pack = 10 pcs)
    @ColumnInfo(name = "cost_price_pack")
    val cost_price_pack: Double = 11500.0, // Harga modal / pabrik per pack
    @ColumnInfo(name = "selling_price_pack")
    val selling_price_pack: Double = 16000.0, // Harga jual konsinyasi per pack ke warung
    @ColumnInfo(name = "cost_price")
    val cost_price: Double = if (pieces_per_pack > 0) cost_price_pack / pieces_per_pack else cost_price_pack,
    @ColumnInfo(name = "selling_price")
    val selling_price: Double = if (pieces_per_pack > 0) selling_price_pack / pieces_per_pack else selling_price_pack
) {
    // Helper formatted display: "30 Pcs (3 Pack)" or "23 Pcs (2 Pack + 3 Pcs)"
    fun formatPackAndPieces(totalPieces: Int): String {
        if (pieces_per_pack <= 1) return "$totalPieces $unit_small"
        val packs = totalPieces / pieces_per_pack
        val remainder = totalPieces % pieces_per_pack
        return when {
            packs > 0 && remainder > 0 -> "$totalPieces $unit_small ($packs $unit_big + $remainder $unit_small)"
            packs > 0 -> "$totalPieces $unit_small ($packs $unit_big)"
            else -> "$totalPieces $unit_small"
        }
    }
}
