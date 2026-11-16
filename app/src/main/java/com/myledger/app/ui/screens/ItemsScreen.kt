package com.myledger.app.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Item
import com.myledger.app.domain.item.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.item.*
import com.myledger.app.ui.viewmodel.*

@Composable fun ItemsScreen(data:LedgerData,vm:LedgerViewModel,modifier:Modifier,dayTick:Long,onAdd:()->Unit){
 var detail by remember{mutableStateOf<Item?>(null)};var editing by remember{mutableStateOf<Item?>(null)};var deleting by remember{mutableStateOf<Item?>(null)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")};var category by remember{mutableStateOf(ItemInvestmentCalculator.AllCategories)};var state by remember{mutableStateOf("")}
 val categories=remember(data.items){ItemInvestmentCalculator.categories(data.items)};val list=data.items.filter{(category==ItemInvestmentCalculator.AllCategories||ItemInvestmentCalculator.categoryOf(it)==category)&&(state.isBlank()||it.status==state)&&listOf(it.name,it.brand,it.model,it.note).any{s->s.contains(query,true)}}
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 item{ScreenHeader("我的物品","好好拥有，让每件物品都值得"){ItemDataActions(vm,compact=true);ActionIcon("plus","添加物品",onAdd)}}
 item{Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){Text(if(category==ItemInvestmentCalculator.AllCategories)"物品总投入"else "${category}投入",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onPrimaryContainer);Text("¥ "+ledgerMoney(ItemInvestmentCalculator.totalInvestmentCent(data.items,category)),style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.onPrimaryContainer);Row{Metric("使用中","${ItemInvestmentCalculator.countByStatus(data.items,category,"使用中")} 件",Modifier.weight(1f));Metric("闲置","${ItemInvestmentCalculator.countByStatus(data.items,category,"闲置")} 件",Modifier.weight(1f));Metric("全部","${data.items.count{category==ItemInvestmentCalculator.AllCategories||ItemInvestmentCalculator.categoryOf(it)==category}} 件",Modifier.weight(1f))}}}}
 item{OutlinedTextField(query,{query=it},placeholder={Text("搜索名称、品牌或型号")},leadingIcon={LedgerIcon("search")},trailingIcon={if(query.isNotBlank())IconButton(onClick={query=""}){LedgerIcon("close")}},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium)}
 item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.forEach{c->FilterChip(selected=category==c,onClick={category=c},label={Text(c)})}}}
 item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${list.size} 件物品",style=MaterialTheme.typography.titleSmall,modifier=Modifier.weight(1f));var menu by remember{mutableStateOf(false)};Box{TextButton(onClick={menu=true}){Text(state.ifBlank{"全部状态"});LedgerIcon("filter",Modifier.padding(start=6.dp).size(16.dp))};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){listOf("","使用中","闲置","已出售","已报废").forEach{s->DropdownMenuItem(text={Text(s.ifBlank{"全部状态"})},onClick={state=s;menu=false})}}}}}
 if(list.isEmpty())item{LedgerEmpty("还没有找到物品","添加一件，看看它陪伴你的每一天。","添加物品",onAdd)}
 items(list,key={it.id}){i->Surface(onClick={detail=i},color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Surface(color=MaterialTheme.colorScheme.surfaceContainerHigh,shape=MaterialTheme.shapes.medium){Box(Modifier.size(52.dp),contentAlignment=Alignment.Center){LedgerIcon("items",Modifier.size(26.dp))}};Column(Modifier.weight(1f)){Text(i.name,style=MaterialTheme.typography.titleMedium);Text(listOf(i.brand,i.category).filter{it.isNotBlank()}.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Surface(color=if(i.status=="使用中")MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,shape=MaterialTheme.shapes.small){Text(i.status,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(horizontal=10.dp,vertical=5.dp))}}
 Row(Modifier.fillMaxWidth()){val days=ItemCostCalculator.usageDays(i.purchaseDate,dayTick);Metric("购入价格","¥ "+ledgerMoney(i.purchasePriceCent),Modifier.weight(1.2f));Metric("已陪伴","${days} 天",Modifier.weight(1f));Metric("日均成本","¥ "+ledgerMoney(i.purchasePriceCent/days),Modifier.weight(1f))}
 }}}
 }
 detail?.let{i->ItemDetailSheet(i,onDismiss={detail=null},onEdit={detail=null;editing=i},onDelete={detail=null;deleting=i})}
 editing?.let{i->ItemEditorSheet(i,isSaving=busy,onDismiss={editing=null},onSave={if(!busy){busy=true;vm.saveItem(it,onError={busy=false;error="保存失败，输入已保留"}){busy=false;editing=null}}})}
 deleting?.let{i->AlertDialog(onDismissRequest={if(!busy)deleting=null},title={Text("删除“${i.name}”？")},text={Text("物品记录删除后无法恢复。")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;vm.deleteItem(i,onError={busy=false;error="删除失败，请重试"}){busy=false;deleting=null}}){Text("删除",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(enabled=!busy,onClick={deleting=null}){Text("保留")}})}
 if(error.isNotBlank())AlertDialog(onDismissRequest={error=""},text={Text(error)},confirmButton={TextButton(onClick={error=""}){Text("知道了")}})
}
