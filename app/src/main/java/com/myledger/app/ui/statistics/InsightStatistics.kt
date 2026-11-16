package com.myledger.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.*
import com.myledger.app.domain.statistics.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.components.charts.*
import com.myledger.app.ui.viewmodel.LedgerData
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun share(value:Long,total:Long):String = if(total<=0)"0%"else BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),1,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()+"%"

@Composable
fun IncomeExpenseAnalysis(data:LedgerData,modifier:Modifier=Modifier,onTransaction:(Transaction)->Unit={}) {
    var period by rememberSaveable{mutableStateOf<BillPeriod?>(BillPeriod.MONTH)}
    var direction by rememberSaveable{mutableStateOf("支出")}
    var reference by rememberSaveable{mutableLongStateOf(System.currentTimeMillis())}
    var calendarExpanded by rememberSaveable{mutableStateOf(false)}
    var initialized by rememberSaveable{mutableStateOf(false)}
    var selected by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(data.transactions.isNotEmpty()){
        if(!initialized&&data.transactions.isNotEmpty()){reference=data.transactions.maxOf{it.date};initialized=true}
    }
    val request=remember(period,reference,direction){StatisticsRequest(period,reference,direction)}
    val computed by produceState<StatisticsSnapshot?>(null,data.transactions,request){
        value=withContext(Dispatchers.Default){LedgerStatistics.calculate(data.transactions,request)}
    }
    val stats=computed?.takeIf{it.request==request}
    val palette=listOf(Color(0xFF42998D),Color(0xFF8EA9D6),Color(0xFFE4AC76),Color(0xFFA69AC7),Color(0xFF91B69E),Color(0xFFCC9498))
    val groups=stats?.groups.orEmpty()
    val scroll=rememberLazyListState()
    val scope=rememberCoroutineScope()
    fun hideCalendar(){calendarExpanded=false;scope.launch{scroll.scrollToItem(0)}}
    fun chooseDate(stamp:Long){reference=stamp;period=BillPeriod.DAY;initialized=true}
    LazyColumn(modifier.fillMaxSize(),state=scroll,contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item(key="periods"){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                (listOf<BillPeriod?>(null)+BillPeriod.entries).forEach{p->FilterChip(selected=period==p,onClick={period=p;initialized=true},label={Text(p?.label?:"累计")},modifier=Modifier.weight(1f))}
            }
        }
        item(key="date-control"){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                ActionIcon("left","统计上一周期"){reference=shiftBillReference(if(calendarExpanded)BillPeriod.MONTH else period?:BillPeriod.MONTH,reference,-1);initialized=true}
                Surface(onClick={if(calendarExpanded)hideCalendar()else calendarExpanded=true},color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,modifier=Modifier.weight(1f).semantics{contentDescription=if(calendarExpanded)"收起统计日历"else "展开统计日历"}){
                    Row(Modifier.padding(horizontal=12.dp,vertical=13.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
                        LedgerIcon("calendar",Modifier.size(18.dp),MaterialTheme.colorScheme.primary)
                        Text(if(calendarExpanded)ledgerDate(reference,"yyyy年M月")else if(period==null)"全部时间"else billPeriodTitle(period!!,reference),Modifier.padding(horizontal=8.dp),style=MaterialTheme.typography.titleSmall)
                        LedgerIcon(if(calendarExpanded)"up"else "down",Modifier.size(14.dp))
                    }
                }
                ActionIcon("right","统计下一周期"){reference=shiftBillReference(if(calendarExpanded)BillPeriod.MONTH else period?:BillPeriod.MONTH,reference,1);initialized=true}
            }
        }
        if(calendarExpanded)item(key="calendar"){
            CalendarDatePanel(data.transactions,reference,onDate=::chooseDate)
        }
        item(key="summary"){
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Row{Text(if(period==null)"累计结余"else "本期结余",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Text("${stats?.records?.size?:0} 笔",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Text(stats?.let{"¥ "+ledgerMoney(it.income-it.expense)}?:"—",style=MaterialTheme.typography.headlineMedium)
                Row{Metric("总收入",stats?.let{"+"+ledgerMoney(it.income)}?:"—",Modifier.weight(1f),MaterialTheme.colorScheme.primary);Metric("总支出",stats?.let{"−"+ledgerMoney(it.expense)}?:"—",Modifier.weight(1f),MaterialTheme.colorScheme.error)}
                if(stats==null)LinearProgressIndicator(Modifier.fillMaxWidth())
            }}
        }
        item(key="direction"){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                listOf("支出","收入").forEach{type->FilterChip(selected=direction==type,onClick={direction=type},label={Text("$type 分析")},modifier=Modifier.weight(1f),leadingIcon={LedgerIcon(if(type=="收入")"download"else "upload",Modifier.size(16.dp))})}
            }
        }
        item(key="composition"){
            SectionHeading("${direction}构成")
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){
                if(stats==null)Box(Modifier.fillMaxWidth().height(160.dp),contentAlignment=Alignment.Center){Text("正在计算…",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                else if(groups.isEmpty())LedgerEmpty("本期暂无${direction}","换个日期看看，或记录第一笔。")
                else Row(Modifier.fillMaxWidth().padding(16.dp).testTag("category-chart"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
                    Box(Modifier.weight(1f).height(150.dp),contentAlignment=Alignment.Center){
                        ExpenseDonutChart(groups.mapIndexed{i,g->g.second to palette[i%palette.size]})
                        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(direction,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("${groups.size} 类",style=MaterialTheme.typography.titleMedium)}
                    }
                    Column(Modifier.weight(1.15f),verticalArrangement=Arrangement.spacedBy(16.dp)){
                        groups.take(3).forEachIndexed{i,(name,value)->Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
                            Row{Text("●",color=palette[i],style=MaterialTheme.typography.labelSmall);Text(name,Modifier.padding(start=6.dp).weight(1f),style=MaterialTheme.typography.titleSmall);Text(share(value,stats!!.total),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                            Text("¥ "+ledgerMoney(value),style=MaterialTheme.typography.bodyMedium)
                        }}
                    }
                }
            }
        }
        item(key="ranking-title"){SectionHeading("分类排行")}
        itemsIndexed(groups,key={_,g->"category-${direction}-${g.first}"}){index,(name,value)->
            Surface(onClick={selected=name},color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text("${index+1}".padStart(2,'0'),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.width(28.dp));LedgerIcon(name,Modifier.size(19.dp),palette[index%palette.size]);Text(name,style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(start=10.dp).weight(1f));Column(horizontalAlignment=Alignment.End){Text("¥ "+ledgerMoney(value),style=MaterialTheme.typography.titleSmall);Text(share(value,stats?.total?:0),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                LinearProgressIndicator(progress={if(stats==null||stats!!.total<=0)0f else value.toFloat()/stats!!.total},modifier=Modifier.fillMaxWidth().height(4.dp),color=palette[index%palette.size],trackColor=MaterialTheme.colorScheme.surfaceContainerHigh)
            }}
        }
        item(key="trend"){
            SectionHeading("${direction}趋势")
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("¥ "+ledgerMoney(stats?.total?:0),style=MaterialTheme.typography.titleLarge)
                if(stats!=null&&stats!!.trend.isNotEmpty())ExpenseLineChart(stats!!.trend)
                else Text("暂无趋势数据",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
                Text(when(period){BillPeriod.DAY->"00:00 — 23:00";BillPeriod.WEEK->"周一 — 周日";BillPeriod.MONTH->"1日 — 月末";BillPeriod.YEAR->"1月 — 12月";null->"按日期汇总"},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
        }
        if(period==BillPeriod.DAY){
            item(key="daily-title"){SectionHeading(ledgerDate(reference,"M月d日 EEEE")+" · 当日明细")}
            if(stats?.records.isNullOrEmpty())item{LedgerEmpty("这一天没有账单","展开日期栏选择其他日期。")}
            else items(stats!!.records,key={"daily-${it.id}"}){record->Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface){TransactionCell(record){onTransaction(record)}}}
        }
    }
    selected?.let{CategoryInsightSheet(category=it,records=stats?.records.orEmpty(),type=direction,onDismiss={selected=null})}
}
