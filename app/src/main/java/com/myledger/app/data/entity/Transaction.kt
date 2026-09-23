package com.myledger.app.data.entity


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["date"]), Index(value = ["type", "date"]), Index(value = ["category", "date"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCent: Long,
    val type: String,
    val category: String,
    val note: String = "",
    val date: Long,
    val relatedItemId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
