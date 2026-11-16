package com.myledger.app.domain.statistics

import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.*
import java.text.SimpleDateFormat
import java.util.*

data class StatisticsRequest(val period: BillPeriod?, val reference: Long, val direction: String)
data class StatisticsSnapshot(val request: StatisticsRequest, val records: List<Transaction>, val income: Long, val expense: Long, val groups: List<Pair<String,Long>>, val trend: List<Long>) {
    val total: Long get() = if(request.direction=="收入")income else expense
}
object LedgerStatistics {
    fun calculate(records:List<Transaction>, request:StatisticsRequest):StatisticsSnapshot {
        val range=request.period?.let{billDateRange(it,request.reference)}
        val filtered=records.filter{range==null||it.date>=range.startMillis&&it.date<range.endExclusiveMillis}
        var income=0L;var expense=0L
        val groups=mutableMapOf<String,Long>()
        val calendar=Calendar.getInstance().apply{timeInMillis=request.reference}
        val size=when(request.period){BillPeriod.DAY->24;BillPeriod.WEEK->7;BillPeriod.MONTH->calendar.getActualMaximum(Calendar.DAY_OF_MONTH);BillPeriod.YEAR->12;null->0}
        val trend=LongArray(size)
        val days=sortedMapOf<String,Long>()
        val format=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT)
        for(t in filtered){
            if(t.type=="收入")income+=t.amountCent else if(t.type=="支出")expense+=t.amountCent
            if(t.type!=request.direction)continue
            groups[t.category]=(groups[t.category]?:0)+t.amountCent
            calendar.timeInMillis=t.date
            val index=when(request.period){BillPeriod.DAY->calendar.get(Calendar.HOUR_OF_DAY);BillPeriod.WEEK->(calendar.get(Calendar.DAY_OF_WEEK)+5)%7;BillPeriod.MONTH->calendar.get(Calendar.DAY_OF_MONTH)-1;BillPeriod.YEAR->calendar.get(Calendar.MONTH);null->-1}
            if(index>=0)trend[index]+=t.amountCent else {val key=format.format(Date(t.date));days[key]=(days[key]?:0)+t.amountCent}
        }
        return StatisticsSnapshot(request,filtered,income,expense,groups.toList().sortedByDescending{it.second},if(request.period==null)days.values.toList()else trend.toList())
    }
}
