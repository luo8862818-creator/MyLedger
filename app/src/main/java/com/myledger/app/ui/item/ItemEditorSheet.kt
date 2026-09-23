package com.myledger.app.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Item
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemEditorSheet(item:Item,onDismiss:()->Unit,onSave:(Item)->Unit){
 var name by remember{mutableStateOf(item.name)};var category by remember{mutableStateOf(item.category)};var brand by remember{mutableStateOf(item.brand)};var model by remember{mutableStateOf(item.model)};var note by remember{mutableStateOf(item.note)};var status by remember{mutableStateOf(item.status)};var price by remember{mutableStateOf((item.purchasePriceCent/100.0).toString())};var date by remember{mutableLongStateOf(item.purchaseDate)};var picker by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 ModalBottomSheet(onDismissRequest=onDismiss){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=onDismiss){Text("取消")};Text("编辑物品",style=MaterialTheme.typography.titleLarge);TextButton(onClick={val cents=parse(price);if(name.isBlank()||cents==null)error="请填写名称和有效价格" else onSave(item.copy(name=name.trim(),category=category.trim().ifBlank{"其他"},brand=brand.trim(),model=model.trim(),note=note.trim(),status=status,purchasePriceCent=cents,purchaseDate=date,updatedAt=System.currentTimeMillis()))}){Text("保存")}};OutlinedTextField(name,{name=it},label={Text("名称 *")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(category,{category=it},label={Text("分类")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(brand,{brand=it},label={Text("品牌")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(model,{model=it},label={Text("型号")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(price,{price=it;error=""},label={Text("购买价格")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),singleLine=true);ListItem(headlineContent={Text("购买日期")},supportingContent={Text(SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA).format(Date(date)))},trailingContent={Text("选择",color=MaterialTheme.colorScheme.primary)},modifier=Modifier.fillMaxWidth().clickable{picker=true});Text("状态");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("使用中","闲置","已出售","已报废").forEach{s->FilterChip(selected=status==s,onClick={status=s},label={Text(s)})}};OutlinedTextField(note,{note=it},label={Text("备注")},modifier=Modifier.fillMaxWidth(),minLines=2);if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)}}
 if(picker){val state=rememberDatePickerState(initialSelectedDateMillis=date);DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.takeIf{it<=System.currentTimeMillis()}?.let{date=it};picker=false}){Text("确定")}},dismissButton={TextButton(onClick={picker=false}){Text("取消")}}){DatePicker(state=state)}}
}
private fun parse(raw:String):Long?=try{if(!Regex("^\\d{1,12}(\\.\\d{1,2})?$").matches(raw))null else {val p=raw.split('.');(p[0].toLong()*100+(p.getOrNull(1)?.padEnd(2,'0')?.toLong()?:0)).takeIf{it>0}}}catch(_:Exception){null}
