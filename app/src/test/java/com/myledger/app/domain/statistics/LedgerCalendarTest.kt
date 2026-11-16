package com.myledger.app.domain.statistics
import com.myledger.app.data.entity.Transaction
import org.junit.Assert.*
import org.junit.Test
import java.util.*
class LedgerCalendarTest {
 private val zone=TimeZone.getTimeZone("Asia/Shanghai")
 private fun at(y:Int,m:Int,d:Int)=LedgerCalendar.at(y,m,d,zone)
 @Test fun leapYearAndMonthBoundaries(){val r=listOf(Transaction(amountCent=123,type="支出",category="餐饮",date=at(2024,2,29)),Transaction(amountCent=456,type="收入",category="工资",date=at(2024,3,1)));val m=LedgerCalendar.month(r,at(2024,2,1),zone);assertEquals(29,m.days.size);assertEquals(3,m.leadingDays);assertEquals(123L,m.expenseCent);assertEquals(0L,m.incomeCent);assertEquals(1,m.days.last().records.size)}
 @Test fun emptyDaysAndBothDirections(){val date=at(2026,11,14);val r=listOf(Transaction(amountCent=73800,type="支出",category="购物",date=date),Transaction(amountCent=96000,type="收入",category="工资",date=date));val m=LedgerCalendar.month(r,date,zone);assertEquals(30,m.days.size);assertEquals(96000L,m.days[13].incomeCent);assertEquals(73800L,m.days[13].expenseCent);assertEquals(0L,m.days[12].expenseCent);assertTrue(m.days[12].records.isEmpty())}
 @Test fun daylightSavingAndClamping(){val z=TimeZone.getTimeZone("America/New_York");val m=LedgerCalendar.month(emptyList(),LedgerCalendar.at(2026,3,1,z),z);assertEquals(23*3600000L,m.days[8].timestamp-m.days[7].timestamp);val c=Calendar.getInstance(zone).apply{timeInMillis=at(2026,2,31)};assertEquals(28,c.get(Calendar.DAY_OF_MONTH))}
 @Test fun manyRecordsSinglePass(){val date=at(2026,11,14);val r=(1..10000).map{Transaction(id=it.toLong(),amountCent=123,type="支出",category="餐饮",date=date)};val m=LedgerCalendar.month(r,date,zone);assertEquals(1230000L,m.expenseCent);assertEquals(10000,m.days[13].records.size)}
}
