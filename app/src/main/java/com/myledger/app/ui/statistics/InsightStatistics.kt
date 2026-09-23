package com.myledger.app.ui.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myledger.app.ui.viewmodel.LedgerData
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InsightStatistics(data: LedgerData, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<String?>(null) }
    val groups=data.transactions.filter { it.type=="支出" }.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amountCent } }.toList().sortedByDescending { it.second }
    LazyColumn(modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("统计",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text("点击分类查看消费频率",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Card { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("累计支出",color=MaterialTheme.colorScheme.onSurfaceVariant); Text(money(groups.sumOf { it.second }),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold) } } }
        item { Text("消费分类",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
        items(groups,key={it.first}) { (name,value) -> Card(modifier=Modifier.fillMaxWidth().clickable { selected=name }) { Column(Modifier.padding(16.dp)) { Text(name,fontWeight=FontWeight.SemiBold); Text("${money(value)} · 点击查看频率与历史",color=MaterialTheme.colorScheme.onSurfaceVariant) } } }
    }
    selected?.let { CategoryInsightSheet(category=it,records=data.transactions,onDismiss={selected=null}) }
}
private fun money(c:Long)=NumberFormat.getCurrencyInstance(Locale.CHINA).format(c/100.0)
