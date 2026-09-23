package com.myledger.app.domain

import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.item.ItemCostCalculator
import com.myledger.app.domain.statistics.StatisticsCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class CalculatorsTest {
    @Test fun `summary and category ratios use integer cents`() {
        val data = listOf(Transaction(amountCent=2550,type="支出",category="餐饮",date=1), Transaction(amountCent=1000,type="支出",category="出行",date=2), Transaction(amountCent=800000,type="收入",category="工资",date=3))
        val s=StatisticsCalculator.summary(data); assertEquals(800000, s.incomeCent); assertEquals(3550, s.expenseCent); assertEquals(796450, s.balanceCent)
        assertEquals(2550, StatisticsCalculator.categories(data).first().amountCent)
        assertNull(StatisticsCalculator.comparison(100, 0))
    }
    @Test fun `purchase day is day one and cost trend has no future points`() {
        val c=Calendar.getInstance().apply { set(2024, Calendar.FEBRUARY, 29, 12, 0, 0) }; assertEquals(1, ItemCostCalculator.usageDays(c.timeInMillis,c.timeInMillis))
        assertEquals(1000, ItemCostCalculator.dailyCostCent(365000,365)); assertTrue(ItemCostCalculator.trend(365000,30).all { it.day <= 30 })
    }
}
