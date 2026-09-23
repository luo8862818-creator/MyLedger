package com.myledger.app.ui.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Item
import com.myledger.app.domain.item.ItemCostCalculator
import com.myledger.app.ui.components.charts.ExpenseLineChart
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemDetailSheet(item: Item, onDismiss:()->Unit) {
 val days=ItemCostCalculator.usageDays(item.purchaseDate); val trend=ItemCostCalculator.trend(item.purchasePriceCent,days).map{it.costCent}; val milestones=listOf(30L,100,365,500,1000)
 ModalBottomSheet(onDismissRequest=onDismiss){LazyColumn(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Text(item.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("${item.category} · ${item.brand.ifBlank{"个人物品"}} · ${item.status}",color=MaterialTheme.colorScheme.onSurfaceVariant)};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("购买价格",money(item.purchasePriceCent));Metric("已陪伴","$days 天");Metric("日均成本",money(item.purchasePriceCent/days))}};item{Text("日均成本变化",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold);ExpenseLineChart(trend)};item{Text("使用里程碑",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold);milestones.forEach{day->Text(if(days>=day) "✓ 已陪伴 $day 天" else "○ 距离 $day 天还有 ${day-days} 天",color=if(days>=day)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}};item{Text("${SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA).format(Date(item.purchaseDate))} 入手",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
}
@Composable private fun Metric(label:String,value:String){Column{Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.SemiBold)}}
private fun money(c:Long)=NumberFormat.getCurrencyInstance(Locale.CHINA).format(c/100.0)
