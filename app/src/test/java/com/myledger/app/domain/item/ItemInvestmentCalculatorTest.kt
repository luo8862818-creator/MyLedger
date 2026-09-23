package com.myledger.app.domain.item

import com.myledger.app.data.entity.Item
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemInvestmentCalculatorTest {
    private val items = listOf(
        Item(name = "手机", category = "数码", purchasePriceCent = 399900, purchaseDate = 1, status = "使用中"),
        Item(name = "电脑", category = "数码", purchasePriceCent = 699900, purchaseDate = 1, status = "闲置"),
        Item(name = "桌子", category = "家居", purchasePriceCent = 120000, purchaseDate = 1, status = "使用中"),
        Item(name = "未分类", category = "", purchasePriceCent = 100, purchaseDate = 1, status = "使用中")
    )

    @Test
    fun `all category includes every item and categories include the fallback`() {
        assertEquals(1_219_900, ItemInvestmentCalculator.totalInvestmentCent(items, ItemInvestmentCalculator.AllCategories))
        assertEquals(listOf("全部", "其他", "家居", "数码"), ItemInvestmentCalculator.categories(items))
    }

    @Test
    fun `category total and status counts only use the selected category`() {
        assertEquals(1_099_800, ItemInvestmentCalculator.totalInvestmentCent(items, "数码"))
        assertEquals(1, ItemInvestmentCalculator.countByStatus(items, "数码", "使用中"))
        assertEquals(1, ItemInvestmentCalculator.countByStatus(items, "数码", "闲置"))
    }
}
