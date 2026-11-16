package com.myledger.app.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.domain.input.ParsedLedgerBatch
import com.myledger.app.ui.components.*

@Composable internal fun SmartEntryPreview(batch:ParsedLedgerBatch){
 if(batch.entries.isEmpty())return
 Surface(color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium){
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   if(batch.entries.size>1)Text("已识别 ${batch.entries.size} 笔",style=MaterialTheme.typography.titleSmall)
   batch.entries.forEachIndexed{index,entry->
    if(index>0)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
      Text("${entry.type?:"待确认收支"} · ${entry.category?:"待确认分类"}",color=MaterialTheme.colorScheme.primary)
      Text(entry.date?.let{ledgerDate(it,"M月d日")}?:"待确认日期",style=MaterialTheme.typography.bodySmall)
     }
     Text(entry.amountCent?.let{"¥ "+ledgerMoney(it)}?:"待识别金额",style=MaterialTheme.typography.headlineSmall)
    }
   }
   if(batch.entries.size>1){
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    if(batch.incomeCent>0)Text("收入合计 ¥ "+ledgerMoney(batch.incomeCent),style=MaterialTheme.typography.bodyMedium)
    if(batch.expenseCent>0)Text("支出合计 ¥ "+ledgerMoney(batch.expenseCent),style=MaterialTheme.typography.bodyMedium)
   }
  }
 }
}
