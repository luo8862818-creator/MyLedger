package com.myledger.app.ui.item
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Item
import com.myledger.app.ui.components.*
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun ItemEditorSheet(item:Item?=null,isSaving:Boolean=false,onDismiss:()->Unit,onSave:(Item)->Unit){
 var name by remember{mutableStateOf(item?.name?:"")};var category by remember{mutableStateOf(item?.category?:"数码")};var brand by remember{mutableStateOf(item?.brand?:"")};var model by remember{mutableStateOf(item?.model?:"")};var note by remember{mutableStateOf(item?.note?:"")};var status by remember{mutableStateOf(item?.status?:"使用中")};var price by remember{mutableStateOf(item?.let{ledgerMoney(it.purchasePriceCent)}?:"")};var date by remember{mutableLongStateOf(item?.purchaseDate?:System.currentTimeMillis())};var warranty by remember{mutableStateOf(item?.warrantyEndDate)};var picker by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf("")}
 fun save(){if(isSaving)return;val cents=runCatching{require(Regex("[0-9]{1,12}(\\.[0-9]{1,2})?").matches(price));BigDecimal(price).movePointRight(2).longValueExact().also{require(it>0)}}.getOrNull();if(name.isBlank()||category.isBlank()||cents==null){error="请填写物品名称、分类和有效价格";return};onSave(Item(id=item?.id?:0,name=name.trim(),category=category.trim(),brand=brand.trim(),model=model.trim(),note=note.trim(),status=status,purchasePriceCent=cents,purchaseDate=date,warrantyEndDate=warranty,createdAt=item?.createdAt?:System.currentTimeMillis(),updatedAt=System.currentTimeMillis()))}
 ModalBottomSheet(onDismissRequest={if(!isSaving)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding()){
 Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){TextButton(enabled=!isSaving,onClick=onDismiss){Text("取消")};Text(if(item==null)"添加物品"else "编辑物品",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))}
 Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 Text("为每一件拥有，留一份记录",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
 Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
 OutlinedTextField(name,{name=it;error=""},label={Text("物品名称 *")},placeholder={Text("例如：每天陪伴我的手机")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 OutlinedTextField(price,{price=it;error=""},prefix={Text("¥")},label={Text("购入价格 *")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),singleLine=true)
 OutlinedTextField(category,{category=it},label={Text("分类 *")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("数码","家电","服饰","家居","其他").forEach{c->SuggestionChip(onClick={category=c},label={Text(c)})}}
 }}
 Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
 SettingsRow("calendar","购买日期",ledgerDate(date,"yyyy年M月d日"),{picker="date"})
 SettingsRow("calendar","保修截止",warranty?.let{ledgerDate(it,"yyyy年M月d日")}?:"选填，方便日后查找",{picker="warranty"})
 if(warranty!=null)TextButton(onClick={warranty=null}){Text("清除保修日期")}
 OutlinedTextField(brand,{brand=it},label={Text("品牌（选填）")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 OutlinedTextField(model,{model=it},label={Text("型号（选填）")},modifier=Modifier.fillMaxWidth(),singleLine=true)
 }}
 Text("目前状态",style=MaterialTheme.typography.titleSmall)
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("使用中","闲置","已出售","已报废").forEach{s->FilterChip(selected=status==s,onClick={status=s},label={Text(s)})}}
 OutlinedTextField(note,{note=it},label={Text("备注（选填）")},modifier=Modifier.fillMaxWidth(),minLines=2)
 if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
 }
 Button(enabled=!isSaving,onClick=::save,modifier=Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=14.dp).heightIn(min=54.dp),shape=MaterialTheme.shapes.medium){Text(if(isSaving)"正在保存…"else "保存物品")}
 }}
 if(picker!=null){val kind=picker;ChineseDatePickerDialog(initialSelectedDateMillis=if(kind=="date")date else warranty?:date,onDismiss={picker=null},onSelected={if(kind=="warranty")warranty=it else if(it<=System.currentTimeMillis())date=it else error="购买日期不能晚于今天"})}
}
