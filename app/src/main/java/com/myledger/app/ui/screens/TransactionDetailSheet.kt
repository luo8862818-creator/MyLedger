package com.myledger.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.ui.components.*
import com.myledger.app.ui.transaction.TransactionEntrySheet
import com.myledger.app.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TransactionDetailSheet(record:Transaction,vm:LedgerViewModel,onDismiss:()->Unit,onChanged:()->Unit){
 var editing by remember{mutableStateOf(false)};var deleting by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 if(editing)TransactionEntrySheet(initial=record,isSaving=busy,onDismiss={editing=false},onSave={if(!busy){busy=true;vm.saveTransactions(it,onError={busy=false;error="保存失败，请重试"}){busy=false;onDismiss();onChanged()}}})
 else ModalBottomSheet(onDismissRequest={if(!busy)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("账单详情",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));ActionIcon("close","关闭详情",onDismiss)}
 Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.size(68.dp),contentAlignment=Alignment.Center){LedgerIcon(record.category,Modifier.size(30.dp),MaterialTheme.colorScheme.primary)}}
 Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){Text(record.category,style=MaterialTheme.typography.titleMedium);Text((if(record.type=="收入")"+ ¥ " else "− ¥ ")+ledgerMoney(record.amountCent),style=MaterialTheme.typography.displaySmall,color=if(record.type=="收入")MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)}
 Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){DetailLine("收支类型",record.type);DetailLine("时间",ledgerDate(record.date,"yyyy年M月d日 HH:mm"));DetailLine("备注",record.note.ifBlank{"未填写"});DetailLine("创建时间",ledgerDate(record.createdAt,"yyyy-MM-dd HH:mm"))}}
 Button(enabled=!busy,onClick={editing=true},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=MaterialTheme.shapes.medium){LedgerIcon("edit",Modifier.size(18.dp),MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(8.dp));Text("编辑账单")}
 TextButton(enabled=!busy,onClick={deleting=true}){Text("删除这笔账单",color=MaterialTheme.colorScheme.error)}
 }}
 if(deleting)AlertDialog(onDismissRequest={if(!busy)deleting=false},title={Text("删除这笔账单？")},text={Text("删除后无法撤销，相关统计也会更新。")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;vm.deleteTransaction(record,onError={busy=false;error="删除失败，请重试"}){busy=false;deleting=false;onDismiss();onChanged()}}){Text(if(busy)"正在删除…"else "删除",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(enabled=!busy,onClick={deleting=false}){Text("保留")}})
 if(error.isNotBlank())AlertDialog(onDismissRequest={error=""},text={Text(error)},confirmButton={TextButton(onClick={error=""}){Text("知道了")}})
}
@Composable fun DetailLine(label:String,value:String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){Text(label,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(.35f));Text(value,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(.65f))}}
