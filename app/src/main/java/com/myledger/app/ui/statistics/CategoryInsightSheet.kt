package com.myledger.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CategoryInsightSheet(category:String, records:List<Transaction>, onDismiss:()->Unit, type:String="支出") {
 val list=records.filter{it.type==type&&it.category==category}.sortedByDescending{it.date}; val gaps=list.zipWithNext{newer,older->(newer.date-older.date)/86_400_000L}.filter{it>=0}; val average=gaps.takeIf{it.isNotEmpty()}?.average()?.toLong(); val last=list.firstOrNull(); val ago=last?.let{maxOf(0,(System.currentTimeMillis()-it.date)/86_400_000L)}
 ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background){LazyColumn(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Text(category,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("${type}频率与历史",color=MaterialTheme.colorScheme.onSurfaceVariant)};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("最近一次",ago?.let{"$it 天前"}?:"暂无");Metric("平均间隔",average?.let{"$it 天"}?:"数据不足");Metric("平均单次",list.takeIf{it.isNotEmpty()}?.let{money(it.sumOf{r->r.amountCent}/it.size)}?:"—")}};item{Text("历史记录",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)};items(list.take(20),key={it.id}){record->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(record.note.ifBlank{category},fontWeight=FontWeight.Medium);Text(SimpleDateFormat("yyyy年MM月dd日 HH:mm",Locale.CHINA).format(Date(record.date)),style=MaterialTheme.typography.bodySmall)};Text("${if(type=="收入")"+" else "−"}${money(record.amountCent)}",color=if(type=="收入")MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)}}}}
}
@Composable private fun Metric(label:String,value:String){Column{Text(label,style=MaterialTheme.typography.labelSmall);Text(value,fontWeight=FontWeight.SemiBold)}}
private fun money(c:Long)=NumberFormat.getCurrencyInstance(Locale.CHINA).format(c/100.0)
