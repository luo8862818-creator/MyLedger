package com.myledger.app.ui.item
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.myledger.app.ui.viewmodel.LedgerViewModel
import com.myledger.app.domain.item.ItemCsvPreview
import kotlinx.coroutines.*

@Composable fun ItemDataActions(vm:LedgerViewModel,compact:Boolean=false) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var payload by remember { mutableStateOf<String?>(null) };var busy by remember { mutableStateOf(false) };var preview by remember { mutableStateOf<ItemCsvPreview?>(null) };var message by remember { mutableStateOf("") }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        val csv=payload;payload=null
        if(uri==null){message="已取消导出"}else scope.launch {busy=true;try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use{it.write(csv?:error("导出内容已失效"))}?:error("文件不可写")};message="物品已导出"}catch(e:Exception){if(e is CancellationException)throw e;message="导出失败：${e.message}"}finally{busy=false}}
    }
    val input=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri==null)message="已取消导入" else scope.launch {busy=true;try{val csv=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.let{java.io.InputStreamReader(it, Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT))}?.use{it.readText()}?:error("文件不可读")};preview=vm.previewItems(csv)}catch(e:Exception){if(e is CancellationException)throw e;message="导入失败：${e.message}"}finally{busy=false}}}
    val doExport:()->Unit={scope.launch{busy=true;try{payload=vm.exportItems();export.launch("MyLedger_物品备份_${java.text.SimpleDateFormat("yyyyMMdd_HHmm",java.util.Locale.CHINA).format(java.util.Date())}.csv")}catch(e:Exception){if(e is CancellationException)throw e;message="导出失败：${e.message}"}finally{busy=false}}}
    val doImport:()->Unit={input.launch(arrayOf("text/*","application/csv"))}
    if(compact){var menu by remember{mutableStateOf(false)};Box{com.myledger.app.ui.components.ActionIcon("more","物品数据操作"){menu=true};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){DropdownMenuItem(text={Text("导出全部物品")},enabled=!busy,onClick={menu=false;doExport()});DropdownMenuItem(text={Text("从 CSV 导入物品")},enabled=!busy,onClick={menu=false;doImport()})}}}
    else Column(verticalArrangement=Arrangement.spacedBy(8.dp)){com.myledger.app.ui.components.SettingsRow("upload","导出物品","导出全部物品为 CSV",doExport,enabled=!busy);com.myledger.app.ui.components.SettingsRow("download","导入物品","预览后导入，不覆盖已有物品",doImport,enabled=!busy);if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())}
    preview?.let { p -> AlertDialog(onDismissRequest={if(!busy)preview=null},title={Text("物品导入预览")},text={Column(Modifier.verticalScroll(rememberScrollState())){Text("新增 ${p.items.size}；疑似重复 ${p.duplicates}；无效 ${p.errors.size}。只导入有效新增记录，其余跳过，不覆盖已有物品。有效记录将在同一事务内写入。");p.errors.forEach{Text(it)}}},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{val n=vm.importItems(p);message="成功 $n；跳过 ${p.duplicates+p.items.size-n}；格式错误 ${p.errors.size}";preview=null}catch(e:Exception){if(e is CancellationException)throw e;message="导入失败，事务已回滚：${e.message}"}finally{busy=false}}}){Text("确认导入")}},dismissButton={TextButton(enabled=!busy,onClick={preview=null}){Text("取消")}}) }
    if(message.isNotBlank())AlertDialog(onDismissRequest={message=""},text={Text(message)},confirmButton={TextButton(onClick={message=""}){Text("确定")}})
}
