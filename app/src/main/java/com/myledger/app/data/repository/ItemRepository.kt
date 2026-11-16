package com.myledger.app.data.repository

import com.myledger.app.data.dao.ItemDao
import com.myledger.app.data.entity.Item

class ItemRepository(private val dao: ItemDao) {
    suspend fun allForExport() = dao.allForExport()
    suspend fun importItems(items: List<Item>) = dao.importItems(items)
    val allItems = dao.getAllItems()
    suspend fun save(item: Item) = if (item.id == 0L) dao.insert(item) else dao.update(item)
    suspend fun delete(item: Item) = dao.delete(item)
}
