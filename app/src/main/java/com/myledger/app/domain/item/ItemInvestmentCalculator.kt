package com.myledger.app.domain.item

import com.myledger.app.data.entity.Item

object ItemInvestmentCalculator {
    const val AllCategories = "全部"
    const val FallbackCategory = "其他"

    fun categoryOf(item: Item): String = item.category.trim().ifBlank { FallbackCategory }

    fun categories(items: List<Item>): List<String> =
        listOf(AllCategories) + items.map(::categoryOf).distinct().sorted()

    fun totalInvestmentCent(items: List<Item>, category: String): Long =
        items.asSequence()
            .filter { category == AllCategories || categoryOf(it) == category }
            .sumOf { it.purchasePriceCent }

    fun countByStatus(items: List<Item>, category: String, status: String): Int =
        items.count { (category == AllCategories || categoryOf(it) == category) && it.status == status }
}
