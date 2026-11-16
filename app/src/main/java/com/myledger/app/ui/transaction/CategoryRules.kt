package com.myledger.app.ui.transaction
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class CategoryRules(context:Context){
 private val prefs=context.getSharedPreferences("ledger_category_rules",Context.MODE_PRIVATE)
 fun read(type:String):Map<String,List<String>> = prefs.all.filterKeys{it.startsWith("$type|")}.mapKeys{it.key.substringAfter('|')}.mapValues{(_,v)->(v as? String).orEmpty().split('、').filter{it.isNotBlank()}}
 fun put(type:String,category:String,terms:String){prefs.edit().putString("$type|${category.trim()}",terms.trim()).apply()}
 fun clear(){prefs.edit().clear().apply()}
}
@Composable fun CategoryRulesEditor(onChanged:()->Unit){
 val context=LocalContext.current;val store=remember{CategoryRules(context)}
 var open by remember{mutableStateOf(false)};var type by remember{mutableStateOf("支出")};var category by remember{mutableStateOf("")};var words by remember{mutableStateOf("")};var message by remember{mutableStateOf("")}
 TextButton(onClick={open=!open}){Text("自定义分类与关键词")}
 if(open)Column{
 Row{listOf("支出","收入").forEach{t->FilterChip(selected=type==t,onClick={type=t},label={Text(t)})}}
 OutlinedTextField(category,{category=it},label={Text("分类名称")},singleLine=true)
 OutlinedTextField(words,{words=it},label={Text("关键词（用顿号分隔）")})
 Row{TextButton(onClick={if(category.isNotBlank()&&words.isNotBlank()){store.put(type,category,words);message="规则已保存在本机";onChanged()}else message="请填写分类与关键词"}){Text("保存规则")};TextButton(onClick={store.clear();message="自定义规则已重置";onChanged()}){Text("重置规则")}}
 Text("用户规则只用于指定收支方向；不会因一次修改自动改变通用词典。",style=MaterialTheme.typography.bodySmall)
 if(message.isNotBlank())Text(message)
 }
}
