package com.myledger.app.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun BillsScreen(vm:LedgerViewModel,modifier:Modifier,isExporting:Boolean,onExport:()->Unit,onTransaction:(Transaction)->Unit){
 var query by rememberSaveable{mutableStateOf("")};var period by rememberSaveable{mutableStateOf(BillPeriod.MONTH)};var type by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf("")};var reference by rememberSaveable{mutableLongStateOf(System.currentTimeMillis())};var filters by remember{mutableStateOf(false)};var pick by remember{mutableStateOf(false)};var searching by remember{mutableStateOf(false)}
 val range=remember(period,reference){billDateRange(period,reference)}
 val raw by remember(range,type,query){vm.billTransactions(range.startMillis,range.endExclusiveMillis,type,query.trim())}.collectAsState(initial=emptyList())
 val list=remember(raw,category){raw.filter{category.isBlank()||it.category==category}};val grouped=remember(list){list.groupBy{ledgerDate(it.date,"yyyy-MM-dd")}};val income=list.filter{it.type=="收入"}.sumOf{it.amountCent};val expense=list.filter{it.type=="支出"}.sumOf{it.amountCent};val scroll=rememberLazyListState()
 LaunchedEffect(range,type,query,category){scroll.scrollToItem(0)}
 LazyColumn(modifier.fillMaxSize(),state=scroll,contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
 item{ScreenHeader("账单","每一笔，都有迹可循"){ActionIcon("search","搜索账单"){searching=!searching};ActionIcon("filter","筛选账单"){filters=true}}}
 if(searching)item{OutlinedTextField(query,{query=it},placeholder={Text("搜索备注或分类")},leadingIcon={LedgerIcon("search")},trailingIcon={IconButton(onClick={query="";searching=false}){LedgerIcon("close")}},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium)}
 item{SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){BillPeriod.entries.forEachIndexed{i,p->SegmentedButton(selected=period==p,onClick={period=p},shape=SegmentedButtonDefaults.itemShape(i,4),icon={},label={Text(p.label)})}}}
 item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){ActionIcon("left","上一周期"){reference=shiftBillReference(period,reference,-1)};Column(Modifier.weight(1f).clickable{pick=true},horizontalAlignment=Alignment.CenterHorizontally){Text(billPeriodTitle(period,reference),style=MaterialTheme.typography.titleMedium);Text("点按更换日期",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};ActionIcon("right","下一周期"){reference=shiftBillReference(period,reference,1)}}}
 item{Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){Row{Text("本期结余",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Text("${list.size} 笔",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("¥ "+ledgerMoney(income-expense),style=MaterialTheme.typography.headlineMedium);Row(Modifier.fillMaxWidth()){Metric("收入","+"+ledgerMoney(income),Modifier.weight(1f),MaterialTheme.colorScheme.primary);Metric("支出","−"+ledgerMoney(expense),Modifier.weight(1f),MaterialTheme.colorScheme.error)}}}}
 if(type.isNotBlank()||category.isNotBlank()||query.isNotBlank())item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){if(type.isNotBlank())InputChip(selected=true,onClick={type="";category=""},label={Text(type)},trailingIcon={LedgerIcon("close",Modifier.size(14.dp))});if(category.isNotBlank())InputChip(selected=true,onClick={category=""},label={Text(category)},trailingIcon={LedgerIcon("close",Modifier.size(14.dp))});if(query.isNotBlank())InputChip(selected=true,onClick={query=""},label={Text(query)},trailingIcon={LedgerIcon("close",Modifier.size(14.dp))})}}
 if(list.isEmpty())item{LedgerEmpty("没有找到相关账单","试试其他日期，或者清除当前筛选。","清除筛选"){query="";type="";category=""}}
 grouped.forEach{(_,records)->
  item{Row(Modifier.fillMaxWidth().padding(top=8.dp)){Text(ledgerDate(records.first().date,"M月d日 EEEE"),style=MaterialTheme.typography.titleSmall,modifier=Modifier.weight(1f));Text("支出 "+ledgerMoney(records.filter{it.type=="支出"}.sumOf{it.amountCent}),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  items(records,key={"record-${it.id}"}){t->Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface){TransactionCell(t){onTransaction(t)}}}
 }
 item{TextButton(enabled=!isExporting,onClick=onExport,modifier=Modifier.fillMaxWidth()){LedgerIcon("upload",Modifier.size(16.dp),MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(if(isExporting)"正在准备…" else "导出全部账单")}}
 }
 if(filters)BillFiltersDialog(period,type,query,category,reference,onApply={p,t,q,c,d->period=p;type=t;query=q;category=c;reference=d},onDismiss={filters=false})
 if(pick) ChineseDatePickerDialog(reference,onDismiss={pick=false},onSelected={reference=it})
}
