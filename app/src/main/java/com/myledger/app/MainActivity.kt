package com.myledger.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myledger.app.data.entity.Transaction
import com.myledger.app.data.backup.*
import com.myledger.app.data.repository.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.screens.*
import com.myledger.app.ui.settings.BackupCenterDialog
import com.myledger.app.ui.theme.MyLedgerTheme
import com.myledger.app.ui.transaction.TransactionEntrySheet
import com.myledger.app.ui.statistics.InsightStatistics
import com.myledger.app.ui.item.ItemEditorSheet
import com.myledger.app.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge()
  val db = (application as MyLedgerApplication).database
  val factory = LedgerViewModelFactory(TransactionRepository(db.transactionDao()), ItemRepository(db.itemDao()))
  setContent { MyLedgerTheme { val vm: LedgerViewModel = viewModel(factory = factory); LedgerApp(vm) } }
 }
}

private enum class Tab(val label: String) { HOME("首页"), BILLS("账单"), ITEMS("物品"), STATS("统计") }
@Composable fun LedgerApp(vm: LedgerViewModel) {
 val tabState=rememberSaveableStateHolder()
 val data by vm.ui.collectAsState(); var tab by rememberSaveable { mutableStateOf(Tab.HOME) }; var addTx by remember { mutableStateOf(false) }; var savingTx by remember { mutableStateOf(false) }; var addItem by remember { mutableStateOf(false) }; var savingItem by remember { mutableStateOf(false) }; var detailTx by remember { mutableStateOf<Transaction?>(null) };var initialInput by remember { mutableStateOf<String?>(null) };var appError by remember { mutableStateOf("") }; var dayTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
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
  if(uri!=null&&!isImporting){isImporting=true;scope.launch { val csv=try{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}?:error("无法读取文件")}}catch(e:Exception){if(e is CancellationException)throw e;isImporting=false;snackbarHostState.showSnackbar("无法读取 CSV 文件");return@launch};vm.importTransactions(csv,onImported={parsed,imported->isImporting=false;automaticBackup();scope.launch{snackbarHostState.showSnackbar("已导入 ${imported} 笔；跳过 ${parsed.transactions.size-imported} 笔，异常 ${parsed.invalidRows} 行")}},onError={isImporting=false;scope.launch{snackbarHostState.showSnackbar("CSV 格式不正确，未导入任何数据")}}) }}
 }
 val automaticBackupFolderLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){ uri ->
  if(uri!=null)try{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);automaticBackupTreeUri=uri.toString();backupPreferences.treeUri=automaticBackupTreeUri;automaticBackup(showSuccess=true)}catch(_:Exception){scope.launch{snackbarHostState.showSnackbar("无法获取该文件夹的长期写入权限")}}
 }
 LaunchedEffect(Unit) { while (true) { val now=Calendar.getInstance(); val next=(now.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,1);set(Calendar.MILLISECOND,0)}; delay(next.timeInMillis-System.currentTimeMillis()); dayTick=System.currentTimeMillis() } }
 Scaffold(
  snackbarHost={ SnackbarHost(snackbarHostState) },
  bottomBar = {
   Surface(color=MaterialTheme.colorScheme.surface,shadowElevation=1.dp){Row(Modifier.fillMaxWidth().navigationBarsPadding().height(80.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
    val order=listOf(Tab.HOME,Tab.BILLS,null,Tab.ITEMS,Tab.STATS)
    order.forEach{t->if(t==null)Box(Modifier.width(80.dp).fillMaxHeight(),contentAlignment=Alignment.Center){Surface(onClick={if(!addTx&&!savingTx){initialInput=null;addTx=true}},modifier=Modifier.size(68.dp).shadow(8.dp,CircleShape,ambientColor=MaterialTheme.colorScheme.primary.copy(alpha=.18f),spotColor=MaterialTheme.colorScheme.primary.copy(alpha=.22f)).semantics{contentDescription="记一笔"},shape=CircleShape,color=MaterialTheme.colorScheme.primary){Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2D9B87),Color(0xFF176760))))){LedgerIcon("plus",Modifier.size(34.dp).align(Alignment.Center),Color.White)}}}else{
     LedgerNavigationItem(icon=when(t){Tab.HOME->"home";Tab.BILLS->"bills";Tab.ITEMS->"items";Tab.STATS->"stats"},label=t.label,isSelected=t==tab,onClick={tab=t},modifier=Modifier.weight(1f))
    }}
   }}
  }
 ) { padding ->
  tabState.SaveableStateProvider(tab.name) { when(tab) {
   Tab.HOME -> HomeScreen(data,dayTick,Modifier.padding(padding),onOpenBackup={showBackupCenter=true},onOpenBills={tab=Tab.BILLS},onOpenStats={tab=Tab.STATS},onAdd={initialInput=null;addTx=true},onVoice={initialInput="voice";addTx=true},onTransaction={detailTx=it})
   Tab.BILLS -> BillsScreen(vm,Modifier.padding(padding),preparingExport,exportAllBills,onTransaction={detailTx=it})
   Tab.ITEMS -> ItemsScreen(data,vm,Modifier.padding(padding),dayTick,onAdd={addItem=true})
   Tab.STATS -> InsightStatistics(data,Modifier.padding(padding),onTransaction={detailTx=it})
  } }
 }
 if(addTx) TransactionEntrySheet(isSaving=savingTx,initialInput=initialInput,onDismiss = { if(!savingTx) addTx = false }, onSave = { batch -> if(!savingTx){ savingTx=true; vm.saveTransactions(batch,onError={savingTx=false;appError="保存失败，请重试，输入已保留"}){ savingTx=false; addTx=false;automaticBackup() } } })
 if(addItem) ItemEditorSheet(isSaving=savingItem,onDismiss={if(!savingItem)addItem=false},onSave={if(!savingItem){savingItem=true;vm.saveItem(it,onError={savingItem=false;appError="保存失败，请重试，输入已保留"}){savingItem=false;addItem=false}}})
 detailTx?.let{TransactionDetailSheet(it,vm,onDismiss={detailTx=null},onChanged={automaticBackup()})}
 if(appError.isNotBlank())AlertDialog(onDismissRequest={appError=""},text={Text(appError)},confirmButton={TextButton(onClick={appError=""}){Text("知道了")}})

 if(showBackupCenter) BackupCenterDialog(vm=vm,automaticBackupEnabled=automaticBackupTreeUri!=null,lastBackupAt=lastAutomaticBackupAt,isImporting=isImporting,isBackingUp=isBackingUp,onDismiss={showBackupCenter=false},onExport=exportAllBills,onImport={importLauncher.launch(arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain"))},onChooseBackupFolder={automaticBackupFolderLauncher.launch(automaticBackupTreeUri?.let{Uri.parse(it)})},onBackupNow={automaticBackup(showSuccess=true)},onDisableAutomaticBackup={automaticBackupTreeUri?.let{uriText->try{context.contentResolver.releasePersistableUriPermission(Uri.parse(uriText),Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}catch(_:Exception){}};backupPreferences.disable();automaticBackupTreeUri=null;lastAutomaticBackupAt=0L})
}
