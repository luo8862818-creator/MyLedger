package com.myledger.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.viewmodel.LedgerData

@Composable fun HomeScreen(data:LedgerData,now:Long,modifier:Modifier,onOpenBackup:()->Unit,onOpenBills:()->Unit,onOpenStats:()->Unit,onAdd:()->Unit,onVoice:()->Unit,onTransaction:(Transaction)->Unit){
 val range=remember(now){billDateRange(BillPeriod.MONTH,now)};val records=remember(data.transactions,range){data.transactions.filter{it.date>=range.startMillis&&it.date<range.endExclusiveMillis}};val income=records.filter{it.type=="收入"}.sumOf{it.amountCent};val expense=records.filter{it.type=="支出"}.sumOf{it.amountCent};val groups=records.filter{it.type=="支出"}.groupBy{it.category}.mapValues{it.value.sumOf{t->t.amountCent}}.toList().sortedByDescending{it.second}
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 item{ScreenHeader("我的账本",ledgerDate(now,"M月d日 EEEE")){ActionIcon("settings","数据与备份",onOpenBackup)}}
 item{Surface(color=Color(0xFF174F55),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF173F4D),Color(0xFF247C74)))).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("本月结余",color=Color(0xFFC9E4DC),style=MaterialTheme.typography.bodyMedium);TextButton(onClick=onOpenStats){Text(ledgerDate(now,"yyyy年M月"),color=Color(0xFFFFD99A))}}
 Text("¥ "+ledgerMoney(income-expense),fontSize=36.sp,lineHeight=44.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFFFFAEF))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)){HeroMetric("本月收入","+"+ledgerMoney(income),Modifier.weight(1f),Color(0xFFB4EDD9));HeroMetric("本月支出","−"+ledgerMoney(expense),Modifier.weight(1f),Color(0xFFFFD2AE))}
 }}}

 item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){QuickAction("plus","记一笔","记录今天的收支",Modifier.weight(1f),onAdd);QuickAction("mic","语音记账","说一句，轻松记",Modifier.weight(1f),onVoice)}}
 item{SectionHeading("最近账单","全部账单",onOpenBills);if(records.isEmpty())Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){LedgerEmpty("从第一笔开始","收入、支出和生活，都值得好好记录。","记一笔",onAdd)}else Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column{records.take(5).forEachIndexed{i,t->TransactionCell(t){onTransaction(t)};if(i<minOf(records.size,5)-1)HorizontalDivider(Modifier.padding(start=74.dp),color=MaterialTheme.colorScheme.outlineVariant)}}}}
 item{SectionHeading("本月支出","查看统计",onOpenStats);Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){if(groups.isEmpty())LedgerEmpty("这个月还没有支出","记下第一笔，看看钱花在哪里。")else Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Row(Modifier.fillMaxWidth()){Metric("累计支出","¥ "+ledgerMoney(expense),Modifier.weight(1f));Metric("账单数量","${records.size} 笔")};val palette=listOf(Color(0xFF4D8F72),Color(0xFFA7BE91),Color(0xFFD4B582),Color(0xFF91ACAC));Row(Modifier.fillMaxWidth().height(8.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){groups.take(3).forEachIndexed{i,(_,v)->Surface(Modifier.weight(v.toFloat().coerceAtLeast(1f)).fillMaxHeight(),color=palette[i],shape=RoundedCornerShape(4.dp)){} };val rest=groups.drop(3).sumOf{it.second};if(rest>0)Surface(Modifier.weight(rest.toFloat()).fillMaxHeight(),color=palette[3],shape=RoundedCornerShape(4.dp)){} };groups.take(3).forEachIndexed{i,(name,v)->Row(verticalAlignment=Alignment.CenterVertically){Surface(Modifier.size(8.dp),shape=RoundedCornerShape(3.dp),color=palette[i]){};Text(name,Modifier.padding(start=8.dp).weight(1f),style=MaterialTheme.typography.bodyMedium);Text("¥ "+ledgerMoney(v),style=MaterialTheme.typography.titleSmall);Text("${if(expense==0L)0 else v*100/expense}%",Modifier.padding(start=12.dp).widthIn(min=38.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}

 }
}
@Composable private fun QuickAction(icon:String,title:String,subtitle:String,modifier:Modifier,onClick:()->Unit){Surface(onClick=onClick,modifier=modifier,shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Row(Modifier.padding(horizontal=14.dp,vertical=15.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){LedgerIcon(icon,color=MaterialTheme.colorScheme.primary);Column{Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))}}}}

@Composable private fun HeroMetric(label:String,value:String,modifier:Modifier,color:Color){Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)){Text(label,style=MaterialTheme.typography.bodySmall,color=Color(0xFFC9E4DC));Text(value,style=MaterialTheme.typography.titleMedium,color=color)}}
