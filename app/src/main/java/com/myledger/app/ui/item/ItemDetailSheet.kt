package com.myledger.app.ui.item
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Item
import com.myledger.app.domain.item.ItemCostCalculator
import com.myledger.app.ui.components.*
import com.myledger.app.ui.components.charts.ExpenseLineChart
import com.myledger.app.ui.screens.DetailLine
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemDetailSheet(item:Item,onDismiss:()->Unit,onEdit:()->Unit={},onDelete:()->Unit={}){
 val days=ItemCostCalculator.usageDays(item.purchaseDate);val trend=ItemCostCalculator.trend(item.purchasePriceCent,days).map{it.costCent}
 ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background){LazyColumn(Modifier.fillMaxWidth(),contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
 item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name,style=MaterialTheme.typography.headlineMedium);Text("${item.category} · ${item.status}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};ActionIcon("close","关闭物品详情",onDismiss)}}
 item{Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){Text("已陪伴你",style=MaterialTheme.typography.bodyMedium);Text("$days 天",style=MaterialTheme.typography.displaySmall);Row{Metric("购入价格","¥ "+ledgerMoney(item.purchasePriceCent),Modifier.weight(1f));Metric("日均成本","¥ "+ledgerMoney(item.purchasePriceCent/days),Modifier.weight(1f))}}}}
 item{Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text("日均成本变化",style=MaterialTheme.typography.titleMedium);ExpenseLineChart(trend);Text("使用越久，每天的拥有成本越低",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
 item{Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){DetailLine("购买日期",ledgerDate(item.purchaseDate,"yyyy年M月d日"));DetailLine("品牌 / 型号",listOf(item.brand,item.model).filter{it.isNotBlank()}.joinToString(" ").ifBlank{"未填写"});DetailLine("保修截止",item.warrantyEndDate?.let{ledgerDate(it,"yyyy年M月d日")}?:"未填写");DetailLine("备注",item.note.ifBlank{"未填写"})}}}
 item{Text("使用里程碑",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(12.dp));Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){listOf(30L,100,365,500,1000).forEach{day->Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){LedgerIcon(if(days>=day)"check"else "calendar",Modifier.size(18.dp),if(days>=day)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline);Text(if(days>=day)"已经一起走过 $day 天"else "距离 $day 天还有 ${day-days} 天",style=MaterialTheme.typography.bodyMedium)}}}}}
 item{Button(onClick=onEdit,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=MaterialTheme.shapes.medium){Text("编辑物品")};TextButton(onClick=onDelete,modifier=Modifier.fillMaxWidth()){Text("删除物品",color=MaterialTheme.colorScheme.error)}}
 }}
}
