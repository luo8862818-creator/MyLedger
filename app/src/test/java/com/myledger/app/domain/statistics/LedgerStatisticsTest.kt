package com.myledger.app.domain.statistics
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*
class LedgerStatisticsTest {
 private fun at(day:Int)=LedgerCalendar.at(2026,11,day)
 private val records=listOf(Transaction(amountCent=1500,type="支出",category="餐饮",date=at(14)),Transaction(amountCent=2800,type="支出",category="出行",date=at(14)),Transaction(amountCent=96000,type="收入",category="工资",date=at(14)),Transaction(amountCent=1000,type="支出",category="餐饮",date=at(15)))
 @Test fun dailyTotalsCategoriesAndTrendAgree(){val s=LedgerStatistics.calculate(records,StatisticsRequest(BillPeriod.DAY,at(14),"支出"));assertEquals(4300L,s.expense);assertEquals(96000L,s.income);assertEquals(4300L,s.groups.sumOf{it.second});assertEquals(4300L,s.trend.sum());assertEquals(3,s.records.size);assertEquals("出行",s.groups.first().first)}
 @Test fun incomeNeverContainsExpenseCategories(){val s=LedgerStatistics.calculate(records,StatisticsRequest(BillPeriod.MONTH,at(14),"收入"));assertEquals(listOf("工资" to 96000L),s.groups);assertEquals(96000L,s.trend.sum());assertEquals(5300L,s.expense)}
 @Test fun allPeriodsAndEmptyAreSafe(){for(p in listOf(null)+BillPeriod.entries){val s=LedgerStatistics.calculate(records,StatisticsRequest(p,at(14),"支出"));assertEquals(s.total,s.trend.sum());assertEquals(s.total,s.groups.sumOf{it.second})};val empty=LedgerStatistics.calculate(emptyList(),StatisticsRequest(BillPeriod.MONTH,at(14),"支出"));assertEquals(0L,empty.total);assertEquals(30,empty.trend.size)}
}
