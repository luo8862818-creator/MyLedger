package com.myledger.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myledger.app.data.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items") suspend fun allForExport(): List<Item>
    @androidx.room.Transaction
    suspend fun importItems(items: List<Item>): Int {
        val seen=allForExport().map(com.myledger.app.domain.item.ItemCsv::key).toMutableSet()
        var count=0
        items.forEach { if(seen.add(com.myledger.app.domain.item.ItemCsv.key(it))){insert(it.copy(id=0));count++} }
        return count
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: Item): Long
    @Update suspend fun update(item: Item)
    @Delete suspend fun delete(item: Item)
    @Query("SELECT * FROM items ORDER BY purchaseDate DESC, id DESC") fun getAllItems(): Flow<List<Item>>
}
