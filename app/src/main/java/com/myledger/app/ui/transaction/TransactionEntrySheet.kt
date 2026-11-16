package com.myledger.app.ui.transaction
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.ui.components.*
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun TransactionEntrySheet(isSaving:Boolean,onDismiss:()->Unit,onSave:(List<Transaction>)->Unit,initial:Transaction?=null,initialInput:String?=null){
 var showRules by remember{mutableStateOf(false)}
 var inputMode by remember{mutableStateOf(initialInput)}
 var amount by remember{mutableStateOf(initial?.let{ledgerMoney(it.amountCent)}?:"")};var type by remember{mutableStateOf(initial?.type?:"支出")};var category by remember{mutableStateOf(initial?.category?:"餐饮")};var note by remember{mutableStateOf(initial?.note?:"")};var date by remember{mutableLongStateOf(initial?.date?:System.currentTimeMillis())};var time by remember{mutableStateOf(ledgerDate(date,"HH:mm"))};var picker by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var revision by remember{mutableIntStateOf(0)}
 val context=LocalContext.current;val rules=remember{CategoryRules(context)}
 val categories=remember(type,revision){((if(type=="收入")listOf("工资","奖金","兼职","退款","其他收入")else listOf("餐饮","出行","加油","停车","购物","居住","娱乐","其他"))+rules.read(type).keys+listOfNotNull(initial?.category?.takeIf{initial.type==type})).distinct()}
 fun submit(){
  if(isSaving)return
  val cents=runCatching{require(Regex("[0-9]{1,12}(\\.[0-9]{1,2})?").matches(amount));BigDecimal(amount).movePointRight(2).longValueExact().also{require(it>0)}}.getOrNull()
  val match=Regex("([01]?\\d|2[0-3]):([0-5]\\d)").matchEntire(time)
  when{cents==null->error="请输入大于零的金额，最多两位小数";type !in listOf("收入","支出")->error="请选择收入或支出";category !in categories->error="请选择分类";match==null->error="请输入有效时间，例如 08:30";else->{val stamp=Calendar.getInstance().apply{timeInMillis=date;set(Calendar.HOUR_OF_DAY,match.groupValues[1].toInt());set(Calendar.MINUTE,match.groupValues[2].toInt());set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis;onSave(listOf(Transaction(id=initial?.id?:0,amountCent=cents,type=type,category=category,note=note.trim(),date=stamp,relatedItemId=initial?.relatedItemId,createdAt=initial?.createdAt?:System.currentTimeMillis(),updatedAt=System.currentTimeMillis())))}}
 }
 ModalBottomSheet(onDismissRequest={if(!isSaving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),sheetGesturesEnabled=false,dragHandle=null,containerColor=MaterialTheme.colorScheme.background){
 CompositionLocalProvider(LocalOverscrollFactory provides null){
 Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).testTag("transaction-sheet")){
  Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){TextButton(enabled=!isSaving,onClick=onDismiss){Text("取消")};Text(if(initial!=null)"编辑账单"else when(inputMode){"text"->"文字记账";"voice"->"语音记账";else->"记一笔"},style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));Text("仅自己可见",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(end=12.dp))}
  if(inputMode!=null){
   SmartEntryInput(mode=inputMode,onModeChange={inputMode=it},isSaving=isSaving,allowMultiple=initial==null,onSave={batch->onSave(batch.map{transaction->transaction.copy(id=initial?.id?:0,relatedItemId=initial?.relatedItemId,createdAt=initial?.createdAt?:transaction.createdAt)})},modifier=Modifier.weight(1f))
  }else{
  Column(Modifier.weight(1f).testTag("transaction-scroll").verticalScroll(rememberScrollState()).padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){SingleChoiceSegmentedButtonRow{listOf("支出","收入").forEachIndexed{i,t->SegmentedButton(selected=type==t,onClick={type=t;category=if(t=="支出")"餐饮" else "工资"},shape=SegmentedButtonDefaults.itemShape(i,2),label={Text(t)},icon={})}}}
   Column(Modifier.fillMaxWidth().padding(vertical=12.dp)){
    Text("${type.ifBlank{"账单"}}金额",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Text("¥",fontSize=28.sp,modifier=Modifier.padding(end=10.dp));BasicTextField(value=amount,onValueChange={amount=it;error=""},singleLine=true,textStyle=MaterialTheme.typography.displaySmall.copy(color=MaterialTheme.colorScheme.onSurface,fontSize=44.sp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),modifier=Modifier.weight(1f).semantics{contentDescription="金额"},decorationBox={inner->Box{if(amount.isBlank())Text("0.00",fontSize=44.sp,color=MaterialTheme.colorScheme.outline);inner()}})}
   }
   SmartEntryInput(mode=null,onModeChange={inputMode=it},isSaving=isSaving,onSave=onSave)
   TextButton(onClick={showRules=!showRules}){Text(if(showRules)"收起分类规则"else "分类识别规则")}
   if(showRules)CategoryRulesEditor{revision++}
   Column{Text("选择分类",style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(bottom=12.dp));BoxWithConstraints{val cell=(maxWidth-24.dp)/4;FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){categories.forEach{c->Column(Modifier.width(cell).semantics{selected=category==c}.clickable{category=c}.padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){Surface(shape=MaterialTheme.shapes.medium,color=if(category==c)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface){Box(Modifier.size(52.dp),contentAlignment=Alignment.Center){LedgerIcon(c,color=if(category==c)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)}};Text(c,style=MaterialTheme.typography.labelMedium,color=if(category==c)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
   Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    Row(Modifier.fillMaxWidth().clickable{picker=true},verticalAlignment=Alignment.CenterVertically){LedgerIcon("calendar");Text("日期",modifier=Modifier.padding(start=10.dp).weight(1f));Text(ledgerDate(date,"yyyy-MM-dd"),style=MaterialTheme.typography.bodyMedium);LedgerIcon("right",Modifier.padding(start=8.dp).size(14.dp))}
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    OutlinedTextField(time,{time=it;error=""},label={Text("时间")},singleLine=true,modifier=Modifier.fillMaxWidth())
    OutlinedTextField(note,{note=it},label={Text("备注")},placeholder={Text("这笔钱用在了哪里？")},modifier=Modifier.fillMaxWidth(),maxLines=3)
   }}
   if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   Spacer(Modifier.height(4.dp))
  }
  Button(enabled=!isSaving,onClick=::submit,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=14.dp).heightIn(min=54.dp)){if(isSaving){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(10.dp))};Text(if(isSaving)"正在保存…" else if(initial==null)"保存账单" else "保存修改")}
  }
 }
 }
 }
 if(picker){
 val utc=TimeZone.getTimeZone("UTC");val local=Calendar.getInstance().apply{timeInMillis=date}
 val selected=Calendar.getInstance(utc).apply{clear();set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH))}.timeInMillis
 ChineseDatePickerDialog(selected,onDismiss={picker=false},onSelected={millis->val c=Calendar.getInstance(utc).apply{timeInMillis=millis};date=Calendar.getInstance().apply{timeInMillis=date;set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH))}.timeInMillis})
 }
}
