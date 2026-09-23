package com.myledger.app.domain.item

import java.util.Calendar
import java.util.TimeZone

data class CostPoint(val day: Long, val costCent: Long)
object ItemCostCalculator {
    /** Uses date fields, not elapsed local milliseconds, so DST cannot change the result. */
    fun usageDays(purchaseDate: Long, today: Long = System.currentTimeMillis()): Long {
        fun utcDate(time: Long): Long { val local=Calendar.getInstance().apply { timeInMillis=time }; return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH)) }.timeInMillis }
        return maxOf(1, (utcDate(today) - utcDate(purchaseDate)) / 86_400_000L + 1)
    }
    fun dailyCostCent(priceCent: Long, days: Long): Long = priceCent / maxOf(1, days)
    fun trend(priceCent: Long, days: Long): List<CostPoint> = listOf(1L,7,30,90,180,365,days).distinct().filter { it <= days }.map { CostPoint(it, dailyCostCent(priceCent,it)) }
}
