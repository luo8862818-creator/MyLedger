package com.myledger.app.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.ui.components.*
import com.myledger.app.ui.item.ItemDataActions
import com.myledger.app.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BackupCenterDialog(vm:LedgerViewModel,automaticBackupEnabled:Boolean,lastBackupAt:Long,isImporting:Boolean,isBackingUp:Boolean,onDismiss:()->Unit,onExport:()->Unit,onImport:()->Unit,onChooseBackupFolder:()->Unit,onBackupNow:()->Unit,onDisableAutomaticBackup:()->Unit){
 ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background){LazyColumn(Modifier.fillMaxWidth(),contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("数据与备份",style=MaterialTheme.typography.headlineMedium);Text("让每一份记录，都安心留存",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))};ActionIcon("close","关闭数据与备份",onDismiss)}}
 item{SectionHeading("账单数据");SettingsRow("upload","导出账单","将全部账单保存为 CSV 文件",onExport)}
 item{SettingsRow("download",if(isImporting)"正在恢复账单…"else "恢复账单","从备份导入，保留现有账单",onImport,enabled=!isImporting)}
 item{SectionHeading("物品数据");ItemDataActions(vm)}
 item{SectionHeading("自动备份");Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
 Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("账单自动备份",style=MaterialTheme.typography.titleMedium);Text(if(automaticBackupEnabled)"已开启" else "未开启",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)};Switch(checked=automaticBackupEnabled,onCheckedChange={if(it)onChooseBackupFolder()else onDisableAutomaticBackup()})}
 Text(if(automaticBackupEnabled)if(lastBackupAt>0)"上次备份："+ledgerDate(lastBackupAt,"M月d日 HH:mm")else "下次账单变更时自动备份" else "选择文件夹，账单变更后自动更新备份文件。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(automaticBackupEnabled)OutlinedButton(enabled=!isBackingUp,onClick=onBackupNow,modifier=Modifier.fillMaxWidth()){Text(if(isBackingUp)"正在备份…"else "立即备份")}else FilledTonalButton(onClick=onChooseBackupFolder,modifier=Modifier.fillMaxWidth()){Text("选择备份文件夹")}
 }}}
 item{Text("账单和物品分别备份。导入不会删除已有记录；建议恢复前先导出当前数据。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))}
 item{TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()){Text("完成")}}
 }}
}
