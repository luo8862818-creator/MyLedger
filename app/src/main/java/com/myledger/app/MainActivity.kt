package com.myledger.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myledger.app.data.entity.Item
import com.myledger.app.data.entity.Transaction
import com.myledger.app.data.backup.BackupPreferences
import com.myledger.app.data.backup.CsvBackupStorage
import com.myledger.app.data.repository.ItemRepository
import com.myledger.app.data.repository.TransactionRepository
import com.myledger.app.domain.bill.BillPeriod
import com.myledger.app.domain.bill.billDateRange
import com.myledger.app.domain.bill.billPeriodDetail
import com.myledger.app.domain.bill.billPeriodTitle
import com.myledger.app.domain.bill.shiftBillReference
import com.myledger.app.domain.item.ItemInvestmentCalculator
import com.myledger.app.ui.theme.MyLedgerTheme
import com.myledger.app.ui.components.charts.ExpenseDonutChart
import com.myledger.app.ui.components.charts.ExpenseLineChart
import com.myledger.app.ui.transaction.TransactionEntrySheet
import com.myledger.app.ui.statistics.CategoryInsightSheet
import com.myledger.app.ui.statistics.InsightStatistics
import com.myledger.app.ui.item.ItemDetailSheet
import com.myledger.app.ui.item.ItemEditorSheet
import com.myledger.app.ui.viewmodel.LedgerData
import com.myledger.app.ui.viewmodel.LedgerViewModel
import com.myledger.app.ui.viewmodel.LedgerViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge()
  val db = (application as MyLedgerApplication).database
  val factory = LedgerViewModelFactory(TransactionRepository(db.transactionDao()), ItemRepository(db.itemDao()))
  setContent { MyLedgerTheme { val vm: LedgerViewModel = viewModel(factory = factory); LedgerApp(vm) } }
 }
}

private enum class Tab(val label: String) { HOME("首页"), BILLS("账单"), ITEMS("物品"), STATS("统计") }
@Composable private fun LedgerApp(vm: LedgerViewModel) {
 val data by vm.ui.collectAsState(); var tab by rememberSaveable { mutableStateOf(Tab.HOME) }; var addTx by remember { mutableStateOf(false) }; var savingTx by remember { mutableStateOf(false) }; var addItem by remember { mutableStateOf(false) }; var dayTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
 val context=LocalContext.current; val scope=rememberCoroutineScope(); val snackbarHostState=remember { SnackbarHostState() }; var exportPayload by remember { mutableStateOf<String?>(null) }; var preparingExport by remember { mutableStateOf(false) }; var showBackupCenter by remember { mutableStateOf(false) }; val backupPreferences=remember(context){BackupPreferences(context.applicationContext)}; var automaticBackupTreeUri by remember { mutableStateOf(backupPreferences.treeUri) }; var lastAutomaticBackupAt by remember { mutableLongStateOf(backupPreferences.lastBackupAt) }; var isBackingUp by remember { mutableStateOf(false) }; var isImporting by remember { mutableStateOf(false) }
 fun automaticBackup(showSuccess:Boolean=false) {
  val uriText=automaticBackupTreeUri ?: return
  if(isBackingUp) return
  isBackingUp=true
  vm.prepareTransactionsCsv(onReady={csv->scope.launch { val saved=runCatching { withContext(Dispatchers.IO) { CsvBackupStorage.writeAutomaticBackup(context,Uri.parse(uriText),csv) } }.getOrDefault(false); isBackingUp=false; if(saved){lastAutomaticBackupAt=System.currentTimeMillis();backupPreferences.lastBackupAt=lastAutomaticBackupAt;if(showSuccess)snackbarHostState.showSnackbar("自动备份已更新")}else snackbarHostState.showSnackbar("自动备份失败，请检查文件夹权限") }},onError={isBackingUp=false;scope.launch{snackbarHostState.showSnackbar("准备自动备份失败，请重试")}})
 }
 val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){ uri ->
  val csv=exportPayload; exportPayload=null
  if(uri!=null&&csv!=null) scope.launch {
   val saved=runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(csv) } ?: error("无法打开所选文件") } }.isSuccess
   snackbarHostState.showSnackbar(if(saved) "账单已导出为 CSV" else "导出失败，请重试")
  }
 }
 val exportAllBills={
 if(!preparingExport) { preparingExport=true; vm.prepareTransactionsCsv(onReady={csv->preparingExport=false;exportPayload=csv;exportLauncher.launch("账单备份_${SimpleDateFormat("yyyyMMdd_HHmm",Locale.CHINA).format(Date())}.csv")},onError={preparingExport=false;scope.launch{snackbarHostState.showSnackbar("准备导出文件失败，请重试")}}) }
 }
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri ->
  if(uri!=null&&!isImporting){isImporting=true;scope.launch { val csv=try{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}?:error("无法读取文件")}}catch(_:Exception){isImporting=false;snackbarHostState.showSnackbar("无法读取 CSV 文件");return@launch};vm.importTransactions(csv,onImported={parsed,imported->isImporting=false;automaticBackup();scope.launch{snackbarHostState.showSnackbar("已导入 ${imported} 笔；跳过 ${parsed.transactions.size-imported} 笔，异常 ${parsed.invalidRows} 行")}},onError={isImporting=false;scope.launch{snackbarHostState.showSnackbar("CSV 格式不正确，未导入任何数据")}}) }}
 }
 val automaticBackupFolderLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){ uri ->
  if(uri!=null)try{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);automaticBackupTreeUri=uri.toString();backupPreferences.treeUri=automaticBackupTreeUri;automaticBackup(showSuccess=true)}catch(_:Exception){scope.launch{snackbarHostState.showSnackbar("无法获取该文件夹的长期写入权限")}}
 }
 LaunchedEffect(Unit) { while (true) { val now=Calendar.getInstance(); val next=(now.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,1);set(Calendar.MILLISECOND,0)}; delay(next.timeInMillis-System.currentTimeMillis()); dayTick=System.currentTimeMillis() } }
 Scaffold(
  snackbarHost={ SnackbarHost(snackbarHostState) },
  bottomBar = {
   NavigationBar {
    Tab.entries.forEach { t ->
     NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = { Text(if (t == Tab.HOME) "⌂" else if (t == Tab.BILLS) "≡" else if (t == Tab.ITEMS) "□" else "⌁") }, label = { Text(t.label) })
    }
   }
  },
  floatingActionButton = { if(tab != Tab.ITEMS) ExtendedFloatingActionButton(onClick = { savingTx=false; addTx = true }, icon={Text("+")}, text={Text("记一笔")}) }
 ) { padding ->
  when(tab) { Tab.HOME -> Home(data, dayTick, Modifier.padding(padding),onOpenBackup={showBackupCenter=true}); Tab.BILLS -> Bills(vm, Modifier.padding(padding),preparingExport,exportAllBills,onTransactionsChanged={automaticBackup()}); Tab.ITEMS -> Items(data, vm, Modifier.padding(padding), dayTick,onAdd={addItem=true}); Tab.STATS -> InsightStatistics(data, Modifier.padding(padding)) }
 }
 if(addTx) TransactionEntrySheet(isSaving=savingTx,onDismiss = { if(!savingTx) addTx = false }, onSave = { transaction -> savingTx=true; vm.saveTransaction(transaction){ savingTx=false; addTx=false;automaticBackup() } })
 if(addItem) ItemDialog(onDismiss = { addItem = false }, onSave = { vm.saveItem(it); addItem = false })
 if(showBackupCenter) BackupCenterDialog(automaticBackupEnabled=automaticBackupTreeUri!=null,lastBackupAt=lastAutomaticBackupAt,isImporting=isImporting,isBackingUp=isBackingUp,onDismiss={showBackupCenter=false},onExport=exportAllBills,onImport={importLauncher.launch(arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain"))},onChooseBackupFolder={automaticBackupFolderLauncher.launch(automaticBackupTreeUri?.let{Uri.parse(it)})},onBackupNow={automaticBackup(showSuccess=true)},onDisableAutomaticBackup={automaticBackupTreeUri?.let{uriText->try{context.contentResolver.releasePersistableUriPermission(Uri.parse(uriText),Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}catch(_:Exception){}};backupPreferences.disable();automaticBackupTreeUri=null;lastAutomaticBackupAt=0L})
}

@Composable private fun BackupCenterDialog(automaticBackupEnabled:Boolean,lastBackupAt:Long,isImporting:Boolean,isBackingUp:Boolean,onDismiss:()->Unit,onExport:()->Unit,onImport:()->Unit,onChooseBackupFolder:()->Unit,onBackupNow:()->Unit,onDisableAutomaticBackup:()->Unit){
 AlertDialog(onDismissRequest=onDismiss,title={Text("数据与备份")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("CSV 可用于留存和恢复账单。导入只新增未存在的记录，不会删除当前账单。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Button(onClick=onExport,modifier=Modifier.fillMaxWidth()){Text("导出全部账单 CSV")}
  OutlinedButton(enabled=!isImporting,onClick=onImport,modifier=Modifier.fillMaxWidth()){Text(if(isImporting)"正在导入…" else "从 CSV 恢复账单")}
  HorizontalDivider(Modifier.padding(vertical=4.dp))
  Text("自动备份",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
  if(automaticBackupEnabled){Text(if(lastBackupAt>0)"已开启 · 上次备份 ${SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA).format(Date(lastBackupAt))}" else "已开启 · 下次账单变更时自动备份",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(enabled=!isBackingUp,onClick=onBackupNow,modifier=Modifier.fillMaxWidth()){Text(if(isBackingUp)"正在备份…" else "立即自动备份")};TextButton(onClick=onDisableAutomaticBackup,modifier=Modifier.fillMaxWidth()){Text("关闭自动备份",color=MaterialTheme.colorScheme.error)}} else {Text("选择文件夹后，每次新增或删除账单都会在该文件夹更新一份完整 CSV。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(onClick=onChooseBackupFolder,modifier=Modifier.fillMaxWidth()){Text("选择自动备份文件夹")}}
 }},confirmButton={TextButton(onClick=onDismiss){Text("完成")}})
}

private enum class Period(val label: String) { WEEK("本周"), MONTH("本月"), YEAR("今年") }
@Composable private fun Home(data: LedgerData, now: Long, modifier: Modifier, onOpenBackup:()->Unit) {
 val monthRange=remember(now){billDateRange(BillPeriod.MONTH,now)}; val tx = data.transactions.filter{it.date>=monthRange.startMillis&&it.date<monthRange.endExclusiveMillis}; val income = tx.filter { it.type == "收入" }.sumOf { it.amountCent }; val expense = tx.filter { it.type == "支出" }.sumOf { it.amountCent }
 val groups=tx.filter{it.type=="支出"}.groupBy{it.category}.mapValues{it.value.sumOf{t->t.amountCent}}.toList().sortedByDescending{it.second}
 LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
  item { Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ Column { Text("我的账本",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text("掌握每一笔钱的去向",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }; Text("⚙",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.clickable(onClick=onOpenBackup)) }; SummaryCard(income,expense) }
  item { Text("支出构成",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold); if(groups.isEmpty()) Empty("暂无分类数据","支出会按分类自动汇总。") else { val palette=listOf(Color(0xFF0E7490),Color(0xFFEA580C),Color(0xFF7C3AED),Color(0xFF16A34A),Color(0xFFDC2626),Color(0xFFCA8A04)); ExpenseDonutChart(groups.take(6).mapIndexed { i,entry -> entry.second to palette[i] }); groups.take(5).forEachIndexed { index,(name,value) -> Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("●  $name",color=palette[index]);Text("${money(value)}  ${value*100/expense}%")}} } }
  item { Text("最近账单",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
  items(tx.take(5), key={it.id}) { TransactionRow(it) }
 }
}
@Composable private fun SummaryCard(income: Long, expense: Long) { Card { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("本月结余"); Text(money(income-expense), style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("收入 ${money(income)}",color=Color(0xFF147D4D)); Text("支出 ${money(expense)}",color=MaterialTheme.colorScheme.error) } } } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Bills(vm: LedgerViewModel, modifier: Modifier, isExporting:Boolean, onExport:()->Unit, onTransactionsChanged:()->Unit) {
 var query by rememberSaveable { mutableStateOf("") }; var deleting by remember { mutableStateOf<Transaction?>(null) }
 var period by rememberSaveable { mutableStateOf(BillPeriod.MONTH) }
 var type by rememberSaveable { mutableStateOf("") }
 var referenceDate by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
 var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
 var showFilters by rememberSaveable { mutableStateOf(false) }
 val range = remember(period, referenceDate) { billDateRange(period, referenceDate) }
 val list by remember(range.startMillis,range.endExclusiveMillis,type,query) { vm.billTransactions(range.startMillis,range.endExclusiveMillis,type,query.trim()) }.collectAsState(initial=emptyList())
 val income=list.filter{it.type=="收入"}.sumOf{it.amountCent}; val expense=list.filter{it.type=="支出"}.sumOf{it.amountCent}
 val scroll=rememberLazyListState()
 LaunchedEffect(range.startMillis,range.endExclusiveMillis,type,query){scroll.scrollToItem(0)}
 Column(modifier.fillMaxSize().padding(20.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("账单", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold);Row{TextButton(enabled=!isExporting,onClick=onExport){Text(if(isExporting)"准备中…" else "导出")};TextButton(onClick={showFilters=true}){Text(if(type.isBlank()&&query.isBlank())"筛选" else "筛选 · 已启用")}}}
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top=14.dp)) {
   BillPeriod.entries.forEachIndexed { index, option ->
    SegmentedButton(selected=period==option,onClick={ period=option },shape=SegmentedButtonDefaults.itemShape(index,BillPeriod.entries.size),label={Text(option.label)})
   }
  }
  Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically) {
   TextButton(onClick={ referenceDate=shiftBillReference(period,referenceDate,-1) }) { Text("‹ 上一${period.label}") }
   Column(Modifier.weight(1f).clickable { showPeriodPicker=true },horizontalAlignment=Alignment.CenterHorizontally) {
    Text(billPeriodTitle(period,referenceDate),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
    Text(when(period){BillPeriod.DAY->"按日查询 · 点按选择日期";BillPeriod.WEEK->billPeriodDetail(range);BillPeriod.MONTH->"按月查询 · 点按选择月份";BillPeriod.YEAR->"按年查询 · 点按选择年份"},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   TextButton(onClick={ referenceDate=shiftBillReference(period,referenceDate,1) }) { Text("下一${period.label} ›") }
  }
  BillFilterSummary(recordCount=list.size,income=income,expense=expense)
  if (list.isEmpty()) Empty("还没有账单", "更换时间范围或筛选条件后再试。") else LazyColumn(modifier=Modifier.weight(1f),state=scroll,verticalArrangement=Arrangement.spacedBy(8.dp)) { list.groupBy { billDayLabel(it.date) }.forEach { (day, records) -> item(key="header-$day") { Text(day,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp)) }; items(records,key={it.id}) { tx -> TransactionRow(tx,onRequestDelete={deleting=tx}) } } }
 }
 deleting?.let { tx -> AlertDialog(onDismissRequest={deleting=null}, title={Text("删除账单？")}, text={Text("此操作无法撤销。")}, confirmButton={TextButton(onClick={vm.deleteTransaction(tx){deleting=null;onTransactionsChanged()}}){Text("删除")}}, dismissButton={TextButton(onClick={deleting=null}){Text("取消")}}) }
 if(showFilters) BillFiltersDialog(period=period,type=type,query=query,referenceDate=referenceDate,onPeriodChange={period=it},onTypeChange={type=it},onQueryChange={query=it},onPickDate={showPeriodPicker=true},onReset={period=BillPeriod.MONTH;type="";query="";referenceDate=System.currentTimeMillis()},onDismiss={showFilters=false})
 if(showPeriodPicker) {
  val pickerState=rememberDatePickerState(initialSelectedDateMillis=referenceDate)
  DatePickerDialog(onDismissRequest={showPeriodPicker=false},confirmButton={TextButton(onClick={pickerState.selectedDateMillis?.let{referenceDate=it};showPeriodPicker=false}){Text("确定")}},dismissButton={TextButton(onClick={showPeriodPicker=false}){Text("取消")}}) {
   DatePicker(state=pickerState)
  }
 }
}
@Composable private fun BillFilterSummary(recordCount:Int,income:Long,expense:Long){Card(modifier=Modifier.fillMaxWidth().padding(top=8.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().padding(10.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("筛选汇总",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold);Text("$recordCount 笔",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)};Row(Modifier.fillMaxWidth().padding(top=5.dp)){BillSummaryAmount("收入",income,Color(0xFF147D4D),Modifier.weight(1f));BillSummaryAmount("支出",expense,MaterialTheme.colorScheme.error,Modifier.weight(1f));BillSummaryAmount("结余",income-expense,MaterialTheme.colorScheme.onPrimaryContainer,Modifier.weight(1f))}}}}
@Composable private fun BillSummaryAmount(label:String,amount:Long,color:Color,modifier:Modifier=Modifier){Column(modifier){Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer);Text(money(amount),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold,color=color)}}
@Composable private fun BillFiltersDialog(period:BillPeriod,type:String,query:String,referenceDate:Long,onPeriodChange:(BillPeriod)->Unit,onTypeChange:(String)->Unit,onQueryChange:(String)->Unit,onPickDate:()->Unit,onReset:()->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("筛选账单")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("统计周期",style=MaterialTheme.typography.labelLarge);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){BillPeriod.entries.forEachIndexed{index,option->SegmentedButton(selected=period==option,onClick={onPeriodChange(option)},shape=SegmentedButtonDefaults.itemShape(index,BillPeriod.entries.size),label={Text(option.label)})}};TextButton(onClick=onPickDate,modifier=Modifier.fillMaxWidth()){Text("${billPeriodTitle(period,referenceDate)}  ·  选择日期")};Text("收支类型",style=MaterialTheme.typography.labelLarge);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf("" to "全部","支出" to "支出","收入" to "收入").forEachIndexed{index,(value,label)->SegmentedButton(selected=type==value,onClick={onTypeChange(value)},shape=SegmentedButtonDefaults.itemShape(index,3),label={Text(label)})}};TextField(value=query,onValueChange=onQueryChange,placeholder={Text("搜索分类或备注")},leadingIcon={Text("⌕",style=MaterialTheme.typography.titleLarge)},singleLine=true,shape=CircleShape,colors=TextFieldDefaults.colors(focusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent),modifier=Modifier.fillMaxWidth().height(54.dp))}},confirmButton={TextButton(onClick=onDismiss){Text("完成")}},dismissButton={TextButton(onClick=onReset){Text("重置")}})}
@Composable private fun TransactionRow(t: Transaction, onRequestDelete:(()->Unit)?=null) { Column(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween) { Row(Modifier.weight(1f)) { Text(categoryGlyph(t.category),style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(end=12.dp)); Column { Text(t.note.ifBlank{t.category},fontWeight=FontWeight.SemiBold); Text("${t.category} · ${dateText(t.date)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } }; Column(horizontalAlignment=Alignment.End) { Text((if(t.type=="支出") "-" else "+")+money(t.amountCent),fontWeight=FontWeight.Bold,color=if(t.type=="支出") MaterialTheme.colorScheme.error else Color(0xFF147D4D)); onRequestDelete?.let { TextButton(onClick=it,contentPadding=PaddingValues(horizontal=0.dp,vertical=0.dp),modifier=Modifier.height(28.dp)){Text("删除",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.error)} } } }; HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f)) } }
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun Items(data: LedgerData, vm: LedgerViewModel, modifier: Modifier, dayTick: Long, onAdd:()->Unit) {
 var deleting by remember { mutableStateOf<Item?>(null) }; var editing by remember { mutableStateOf<Item?>(null) }; var detail by remember { mutableStateOf<Item?>(null) }; var selected by remember { mutableStateOf(ItemInvestmentCalculator.AllCategories) }; var query by remember { mutableStateOf("") }
 val categories=remember(data.items){ItemInvestmentCalculator.categories(data.items)}; LaunchedEffect(categories){if(selected !in categories)selected=ItemInvestmentCalculator.AllCategories}; val selectedInvestment=remember(data.items,selected){ItemInvestmentCalculator.totalInvestmentCent(data.items,selected)}; val shown=data.items.filter { (selected==ItemInvestmentCalculator.AllCategories||ItemInvestmentCalculator.categoryOf(it)==selected) && listOf(it.name,it.brand,it.model,it.note).any { text -> text.contains(query,true) } }
 LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("我的物品",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("记录每一件陪伴你的物品",color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton(onClick=onAdd){Text("+ 添加")}}; Spacer(Modifier.height(18.dp)); Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=MaterialTheme.shapes.extraLarge) { Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween) { Column { Text(if(selected==ItemInvestmentCalculator.AllCategories)"总投入" else "${selected}投入",color=MaterialTheme.colorScheme.onPrimaryContainer); Text(money(selectedInvestment),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold) }; Column(horizontalAlignment=androidx.compose.ui.Alignment.End) { Text("使用中 ${ItemInvestmentCalculator.countByStatus(data.items,selected,"使用中")}",fontWeight=FontWeight.SemiBold); Text("闲置 ${ItemInvestmentCalculator.countByStatus(data.items,selected,"闲置")}",color=MaterialTheme.colorScheme.onPrimaryContainer) } } }; OutlinedTextField(query,{query=it},label={Text("搜索名称、品牌或型号")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(top=14.dp)); FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { categories.forEach { cat -> FilterChip(selected=selected==cat,onClick={selected=cat},label={Text(cat,maxLines=1,softWrap=false)}) } } }
  if(shown.isEmpty()) item { Empty("还没有记录物品","添加手机、电脑等物品后，可以查看使用时间和日均成本。") }
  items(shown,key={it.id}) { item -> AssetCard(item,onOpen={detail=item},onEdit={editing=item},onDelete={deleting=item},today=dayTick) }
 }
 deleting?.let { item -> AlertDialog(onDismissRequest={deleting=null},title={Text("删除物品？")},confirmButton={TextButton(onClick={vm.deleteItem(item);deleting=null}){Text("删除")}},dismissButton={TextButton(onClick={deleting=null}){Text("取消")}}) }
 editing?.let { item -> ItemEditorSheet(item,onDismiss={editing=null},onSave={vm.saveItem(it);editing=null}) }
 detail?.let { item -> ItemDetailSheet(item,onDismiss={detail=null}) }
}
@Composable private fun AssetCard(item: Item, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, today: Long) { val days=usageDays(item.purchaseDate,today); Card(modifier=Modifier.clickable{onOpen()},shape=MaterialTheme.shapes.large,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f))) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Column { Text(item.name,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold); Text(listOf(item.brand,item.category).filter{it.isNotBlank()}.joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant) }; Text(item.status,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge) }; Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Metric("购入价格",money(item.purchasePriceCent)); Metric("已陪伴","$days 天"); Metric("日均",money(item.purchasePriceCent/days)) }; Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("${SimpleDateFormat("yyyy.MM.dd",Locale.CHINA).format(Date(item.purchaseDate))} 入手",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); Row { Text("编辑",modifier=Modifier.clickable{onEdit()},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Text("删除",modifier=Modifier.clickable{onDelete()},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.error) } } } } }
@Composable private fun Metric(label:String,value:String) { Column { Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant); Text(value,fontWeight=FontWeight.SemiBold) } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun EditItemDialog(item: Item, onDismiss:()->Unit, onSave:(Item)->Unit) { var name by remember { mutableStateOf(item.name) }; var category by remember { mutableStateOf(item.category) }; var price by remember { mutableStateOf((item.purchasePriceCent / 100.0).toString()) }; var date by remember { mutableLongStateOf(item.purchaseDate) }; var picker by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; AlertDialog(onDismissRequest=onDismiss,title={Text("编辑物品")},text={Column { OutlinedTextField(name,{name=it},label={Text("名称")},singleLine=true); OutlinedTextField(category,{category=it},label={Text("分类")},singleLine=true); OutlinedTextField(price,{price=it;error=""},label={Text("购买价格")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true); ListItem(headlineContent={Text("购买日期")},supportingContent={Text(SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA).format(Date(date)))},trailingContent={Text("选择",color=MaterialTheme.colorScheme.primary)},modifier=Modifier.clickable{picker=true}); if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error) }},confirmButton={TextButton(onClick={val cents=parseCents(price);if(name.isBlank()||cents==null)error="请填写有效名称和价格" else onSave(item.copy(name=name.trim(),category=category.trim().ifBlank{"其他"},purchasePriceCent=cents,purchaseDate=date,updatedAt=System.currentTimeMillis()))}){Text("保存")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}}); if(picker){val state=rememberDatePickerState(initialSelectedDateMillis=date);DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.takeIf{it<=System.currentTimeMillis()}?.let{date=it};picker=false}){Text("确定")}},dismissButton={TextButton(onClick={picker=false}){Text("取消")}}){DatePicker(state=state)}} }
@Composable private fun Statistics(data: LedgerData, modifier: Modifier) { var period by remember { mutableStateOf(Period.MONTH) }; val tx=data.transactions.filter{inPeriod(it.date,period)}; val income=tx.filter{it.type=="收入"}.sumOf{it.amountCent}; val expense=tx.filter{it.type=="支出"}.sumOf{it.amountCent}; val groups=tx.filter{it.type=="支出"}.groupBy{it.category}.mapValues{it.value.sumOf{t->t.amountCent}}.toList().sortedByDescending{it.second}; val count=if(period==Period.MONTH)Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) else if(period==Period.YEAR)12 else 7; val trend=(1..count).map { n -> tx.filter { t -> val c=Calendar.getInstance().apply{timeInMillis=t.date}; t.type=="支出" && if(period==Period.YEAR)c.get(Calendar.MONTH)+1==n else if(period==Period.MONTH)c.get(Calendar.DAY_OF_MONTH)==n else c.get(Calendar.DAY_OF_WEEK)==n }.sumOf{it.amountCent} }; LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) { item { Spacer(Modifier.height(10.dp)); Text("统计",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text("看见支出结构与变化",color=MaterialTheme.colorScheme.onSurfaceVariant); SingleChoiceSegmentedButtonRow(Modifier.padding(top=16.dp)){Period.entries.forEachIndexed{i,p->SegmentedButton(selected=period==p,onClick={period=p},shape=SegmentedButtonDefaults.itemShape(i,3),label={Text(p.label)})}}; Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=MaterialTheme.shapes.extraLarge){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("${period.label}结余");Text(money(income-expense),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("收入",money(income));Metric("支出",money(expense));Metric("日均",money(expense/maxOf(1,count)))}}} }; item { Text("支出变化",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold); if(expense==0L)Empty("本期暂无支出","添加账单后即可查看变化。") else ExpenseLineChart(trend) }; item { Text("分类排行",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }; items(groups.take(6)){(name,value)->Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text("${money(value)}  ${if(expense==0L)0 else value*100/expense}%")};LinearProgressIndicator(progress={if(expense==0L)0f else value.toFloat()/expense},modifier=Modifier.fillMaxWidth().padding(top=6.dp))}} } }
@Composable private fun Empty(title:String, detail:String) { Column(Modifier.fillMaxWidth().padding(top=48.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) { Text(title,style=MaterialTheme.typography.titleLarge); Text(detail,style=MaterialTheme.typography.bodyMedium) } }
@Composable private fun TransactionDialog(onDismiss:()->Unit,onSave:(Transaction)->Unit) { var amount by remember{mutableStateOf("")}; var type by remember{mutableStateOf("支出")}; var category by remember{mutableStateOf("餐饮")}; var note by remember{mutableStateOf("")}; var error by remember{mutableStateOf("")}; AlertDialog(onDismissRequest=onDismiss,title={Text("记一笔")},text={Column { OutlinedTextField(amount,{amount=it;error=""},label={Text("金额")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true); Row { FilterChip(type=="支出",{type="支出"},{Text("支出")}); Spacer(Modifier.width(8.dp)); FilterChip(type=="收入",{type="收入"},{Text("收入")}) }; OutlinedTextField(category,{category=it},label={Text("分类")},singleLine=true); OutlinedTextField(note,{note=it},label={Text("备注（可选）")},singleLine=true); if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error) }},confirmButton={TextButton({ val c=parseCents(amount); if(c==null) error="请输入不超过两位小数的有效金额" else onSave(Transaction(amountCent=c,type=type,category=category.trim().ifBlank{"其他"},note=note.trim(),date=System.currentTimeMillis())) }){Text("保存")}},dismissButton={TextButton(onDismiss){Text("取消")}}) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ItemDialog(onDismiss:()->Unit,onSave:(Item)->Unit) {
 var name by remember{mutableStateOf("")}; var price by remember{mutableStateOf("")}; var category by remember{mutableStateOf("数码")}; var error by remember{mutableStateOf("")}; var purchaseDate by remember{mutableLongStateOf(System.currentTimeMillis())}; var picker by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("添加物品")},text={Column { OutlinedTextField(name,{name=it;error=""},label={Text("物品名称 *")},singleLine=true); OutlinedTextField(category,{category=it},label={Text("分类")},singleLine=true); OutlinedTextField(price,{price=it;error=""},label={Text("购买价格 *")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true); ListItem(headlineContent={Text("购买日期 *")},supportingContent={Text(SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA).format(Date(purchaseDate)))},trailingContent={Text("›")},modifier=Modifier.clickable{picker=true}); if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error) }},confirmButton={TextButton({val p=parseCents(price); if(name.isBlank()||p==null) error="请填写名称和有效价格" else onSave(Item(name=name.trim(),category=category.trim().ifBlank{"其他"},purchasePriceCent=p,purchaseDate=purchaseDate))}){Text("保存")}},dismissButton={TextButton(onDismiss){Text("取消")}})
 if(picker) { val state=rememberDatePickerState(initialSelectedDateMillis=purchaseDate); DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={ state.selectedDateMillis?.takeIf { it <= System.currentTimeMillis() }?.let{purchaseDate=it}; picker=false }){Text("确定")}},dismissButton={TextButton(onClick={picker=false}){Text("取消")}}){ DatePicker(state=state) } }
}
private fun money(c:Long)=NumberFormat.getCurrencyInstance(Locale.CHINA).format(c/100.0)
private fun parseCents(raw:String):Long? { if(!Regex("^\\d{1,12}(\\.\\d{1,2})?$").matches(raw)) return null; val parts=raw.split('.'); return try { Math.addExact(Math.multiplyExact(parts[0].toLong(),100),parts.getOrNull(1)?.padEnd(2,'0')?.toLong()?:0).takeIf{it>0} } catch(_:Exception){null} }
private fun dateText(time:Long)=SimpleDateFormat("MM月dd日",Locale.CHINA).format(Date(time))
private fun categoryGlyph(category:String)=when(category){"餐饮"->"🍜";"出行"->"🚇";"加油"->"⛽";"停车"->"🅿";"购物"->"🛍";"工资"->"¥";"居住","其他","其它","其他收入"->"·";else->"·"}
private fun billDayLabel(time:Long):String { fun localDay(value:Long):Long { val calendar=Calendar.getInstance(); calendar.timeInMillis=value; calendar.set(Calendar.HOUR_OF_DAY,0); calendar.set(Calendar.MINUTE,0); calendar.set(Calendar.SECOND,0); calendar.set(Calendar.MILLISECOND,0); return calendar.timeInMillis }; val day=(localDay(System.currentTimeMillis())-localDay(time))/86_400_000L; return when(day){0L->"今天 · "+SimpleDateFormat("MM月dd日",Locale.CHINA).format(Date(time));1L->"昨天 · "+SimpleDateFormat("MM月dd日",Locale.CHINA).format(Date(time));else->SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA).format(Date(time))} }
private fun inPeriod(time:Long,p:Period):Boolean { val now=Calendar.getInstance(); val start=(now.clone() as Calendar); when(p){Period.WEEK->{start.firstDayOfWeek=Calendar.MONDAY; start.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY)};Period.MONTH->start.set(Calendar.DAY_OF_MONTH,1);Period.YEAR->start.set(Calendar.DAY_OF_YEAR,1)}; start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);return time>=start.timeInMillis&&time<now.timeInMillis }
private fun usageDays(purchase:Long, now:Long=System.currentTimeMillis()):Long { val local=Calendar.getInstance(); local.timeInMillis=purchase; val utc=Calendar.getInstance(TimeZone.getTimeZone("UTC")); utc.clear(); utc.set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH)); val today=Calendar.getInstance(); today.timeInMillis=now; val end=Calendar.getInstance(TimeZone.getTimeZone("UTC")); end.clear(); end.set(today.get(Calendar.YEAR),today.get(Calendar.MONTH),today.get(Calendar.DAY_OF_MONTH)); return maxOf(1,(end.timeInMillis-utc.timeInMillis)/86_400_000+1) }
