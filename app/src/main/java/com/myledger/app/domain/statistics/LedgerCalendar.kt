package com.myledger.app.domain.statistics

import com.myledger.app.data.entity.Transaction
import java.util.Calendar
import java.util.TimeZone

data class LedgerDay(val day: Int, val timestamp: Long, val incomeCent: Long, val expenseCent: Long, val records: List<Transaction>)
data class LedgerMonth(val year: Int, val month: Int, val leadingDays: Int, val days: List<LedgerDay>) {
    val incomeCent: Long get() = days.sumOf { it.incomeCent }
    val expenseCent: Long get() = days.sumOf { it.expenseCent }
}

/** Uses local calendar boundaries, including short/long days at daylight-saving changes. */
object LedgerCalendar {
    fun month(records: List<Transaction>, reference: Long, zone: TimeZone = TimeZone.getDefault()): LedgerMonth {
        val start = Calendar.getInstance(zone).apply {
            timeInMillis = reference
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
        val calendar = Calendar.getInstance(zone)
        val grouped = records.asSequence().filter { it.date >= start.timeInMillis && it.date < end }
            .groupBy { calendar.timeInMillis = it.date; calendar.get(Calendar.DAY_OF_MONTH) }
        val days = (1..start.getActualMaximum(Calendar.DAY_OF_MONTH)).map { day ->
            val stamp = (start.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
            val list = grouped[day].orEmpty().sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
            LedgerDay(day, stamp, list.filter { it.type == "收入" }.sumOf { it.amountCent }, list.filter { it.type == "支出" }.sumOf { it.amountCent }, list)
        }
        return LedgerMonth(start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, (start.get(Calendar.DAY_OF_WEEK) + 5) % 7, days)
    }
    fun at(year: Int, month: Int, day: Int = 1, zone: TimeZone = TimeZone.getDefault()): Long = Calendar.getInstance(zone).apply {
        clear(); set(year, month - 1, 1); set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
    }.timeInMillis
}
