package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val phone: String,
    @ColumnInfo(name = "route_day")
    val route_day: String = "Senin", // Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, Minggu
    @ColumnInfo(name = "route_order")
    val route_order: Int = 1,
    @ColumnInfo(name = "photo_uri")
    val photo_uri: String? = null,
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    @ColumnInfo(name = "created_at")
    val created_at: Long = System.currentTimeMillis()
)
