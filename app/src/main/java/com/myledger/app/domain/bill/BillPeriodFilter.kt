package com.myledger.app.domain.bill

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The range chosen on the bill screen. The end is always exclusive. */
data class BillDateRange(val startMillis: Long, val endExclusiveMillis: Long)

enum class BillPeriod(val label: String) {
    DAY("日"),
    WEEK("周"),
    MONTH("月"),
    YEAR("年")
}

/**
 * Gets the calendar day, Monday-based week, calendar month, or calendar year containing [referenceMillis].
 * Keeping the end exclusive makes records at 00:00 on the following period unambiguous.
 */
fun billDateRange(
    period: BillPeriod,
    referenceMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): BillDateRange {
    val start = Calendar.getInstance(timeZone).apply {
        timeInMillis = referenceMillis
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (period) {
            BillPeriod.DAY -> Unit
            BillPeriod.WEEK -> set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            BillPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
            BillPeriod.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val end = (start.clone() as Calendar).apply {
        when (period) {
            BillPeriod.DAY -> add(Calendar.DAY_OF_YEAR, 1)
            BillPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
            BillPeriod.MONTH -> add(Calendar.MONTH, 1)
            BillPeriod.YEAR -> add(Calendar.YEAR, 1)
        }
    }
    return BillDateRange(start.timeInMillis, end.timeInMillis)
}

fun shiftBillReference(
    period: BillPeriod,
    referenceMillis: Long,
    offset: Int,
    timeZone: TimeZone = TimeZone.getDefault()
): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = referenceMillis
    when (period) {
        BillPeriod.DAY -> add(Calendar.DAY_OF_YEAR, offset)
        BillPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, offset)
        BillPeriod.MONTH -> add(Calendar.MONTH, offset)
        BillPeriod.YEAR -> add(Calendar.YEAR, offset)
    }
}.timeInMillis

fun billPeriodTitle(
    period: BillPeriod,
    referenceMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = referenceMillis
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }
    return when (period) {
        BillPeriod.DAY -> SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).apply { this.timeZone = timeZone }.format(Date(referenceMillis))
        BillPeriod.WEEK -> "${calendar.get(Calendar.YEAR)}年${calendar.get(Calendar.MONTH) + 1}月 · 第${maxOf(1, calendar.get(Calendar.WEEK_OF_MONTH))}周"
        BillPeriod.MONTH -> SimpleDateFormat("yyyy年MM月", Locale.CHINA).apply { this.timeZone = timeZone }.format(Date(referenceMillis))
        BillPeriod.YEAR -> SimpleDateFormat("yyyy年", Locale.CHINA).apply { this.timeZone = timeZone }.format(Date(referenceMillis))
    }
}

fun billPeriodDetail(range: BillDateRange, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat("MM月dd日", Locale.CHINA).apply { this.timeZone = timeZone }
    val lastMoment = range.endExclusiveMillis - 1
    return "${formatter.format(Date(range.startMillis))} - ${formatter.format(Date(lastMoment))}"
}
