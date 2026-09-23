package com.myledger.app.domain.bill

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BillPeriodFilterTest {
    private val utc = TimeZone.getTimeZone("UTC")

    private fun timestamp(year: Int, month: Int, day: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month - 1, day, 14, 0, 0)
    }.timeInMillis

    private fun midnight(year: Int, month: Int, day: Int, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(year, month - 1, day, 0, 0, 0)
    }.timeInMillis

    @Test
    fun `week uses monday through sunday even when it crosses a month`() {
        val range = billDateRange(BillPeriod.WEEK, timestamp(2026, 9, 1), utc)

        assertEquals(timestamp(2026, 8, 31) - 14 * 60 * 60 * 1000, range.startMillis)
        assertEquals(timestamp(2026, 9, 7) - 14 * 60 * 60 * 1000, range.endExclusiveMillis)
        assertEquals("08月31日 - 09月06日", billPeriodDetail(range, utc))
    }

    @Test
    fun `month and year have calendar boundaries`() {
        val reference = timestamp(2024, 2, 29)
        val month = billDateRange(BillPeriod.MONTH, reference, utc)
        val year = billDateRange(BillPeriod.YEAR, reference, utc)

        assertEquals(timestamp(2024, 2, 1) - 14 * 60 * 60 * 1000, month.startMillis)
        assertEquals(timestamp(2024, 3, 1) - 14 * 60 * 60 * 1000, month.endExclusiveMillis)
        assertEquals(timestamp(2024, 1, 1) - 14 * 60 * 60 * 1000, year.startMillis)
        assertEquals(timestamp(2025, 1, 1) - 14 * 60 * 60 * 1000, year.endExclusiveMillis)
    }

    @Test
    fun `period navigation keeps the selected period`() {
        assertEquals(timestamp(2025, 2, 28), shiftBillReference(BillPeriod.YEAR, timestamp(2024, 2, 29), 1, utc))
        assertEquals(timestamp(2024, 3, 29), shiftBillReference(BillPeriod.MONTH, timestamp(2024, 2, 29), 1, utc))
    }

    @Test
    fun `a date picker day is filtered as the selected China calendar day`() {
        val china = TimeZone.getTimeZone("Asia/Shanghai")
        // Material DatePicker supplies UTC midnight for the selected date.
        val pickerValue = midnight(2026, 9, 21, utc)
        val range = billDateRange(BillPeriod.DAY, pickerValue, china)

        assertEquals(midnight(2026, 9, 21, china), range.startMillis)
        assertEquals(midnight(2026, 9, 22, china), range.endExclusiveMillis)
        assertEquals("2026年09月21日", billPeriodTitle(BillPeriod.DAY, pickerValue, china))
    }
}
