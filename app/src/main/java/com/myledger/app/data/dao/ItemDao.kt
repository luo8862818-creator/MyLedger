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
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: Item): Long
    @Update suspend fun update(item: Item)
    @Delete suspend fun delete(item: Item)
    @Query("SELECT * FROM items ORDER BY purchaseDate DESC, id DESC") fun getAllItems(): Flow<List<Item>>
}
