package com.myledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "items", indices = [Index(value = ["category"]), Index(value = ["status"]), Index(value = ["purchaseDate"])])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val brand: String = "",
    val model: String = "",
    val purchasePriceCent: Long,
    val purchaseDate: Long,
    val warrantyEndDate: Long? = null,
    val note: String = "",
    val status: String = "使用中",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
