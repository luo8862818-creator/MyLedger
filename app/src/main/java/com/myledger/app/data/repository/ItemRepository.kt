package com.myledger.app.data.repository

import com.myledger.app.data.dao.ItemDao
import com.myledger.app.data.entity.Item

class ItemRepository(private val dao: ItemDao) {
    val allItems = dao.getAllItems()
    suspend fun save(item: Item) = if (item.id == 0L) dao.insert(item) else dao.update(item)
    suspend fun delete(item: Item) = dao.delete(item)
}
