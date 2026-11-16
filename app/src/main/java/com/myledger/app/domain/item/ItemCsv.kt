package com.myledger.app.domain.item

import com.myledger.app.data.entity.Item
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.*

data class ItemCsvPreview(val items: List<Item>, val errors: List<String>, val duplicates: Int)
object ItemCsv {
    val headers=listOf("记录ID","名称","分类","品牌","型号","购买价格（元）","购买日期","保修截止日期","备注","状态","创建时间","修改时间")
    private fun dateFormat()=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).apply { isLenient=false }
    fun key(i:Item)=listOf(i.name,i.category,i.brand,i.model,dateFormat().format(Date(i.purchaseDate)),i.purchasePriceCent.toString())
    fun format(items:List<Item>):String {
        val f=dateFormat()
        fun row(values:List<String>)=values.joinToString(",") { "\""+it.replace("\"","\"\"")+"\"" }
        return "\uFEFF"+(listOf(row(headers))+items.map { i -> row(listOf(i.id.toString(),i.name,i.category,i.brand,i.model,BigDecimal.valueOf(i.purchasePriceCent,2).toPlainString(),f.format(Date(i.purchaseDate)),i.warrantyEndDate?.let{f.format(Date(it))}.orEmpty(),i.note,i.status,i.createdAt.toString(),i.updatedAt.toString())) }).joinToString("\r\n")
    }
    fun parse(source:String,existing:List<Item>):ItemCsvPreview {
        val rows=rows(source.removePrefix("\uFEFF")); require(rows.isNotEmpty()) { "文件为空" }; require(rows.first()==headers) { "请使用本应用导出的物品 CSV 模板" }
        val seen=existing.map(::key).toMutableSet(); val items=mutableListOf<Item>();val errors=mutableListOf<String>();var duplicates=0
        val f=dateFormat()
        fun date(s:String):Long { val pos=ParsePosition(0);return f.parse(s,pos)?.takeIf{pos.index==s.length}?.time?:error("日期无效：$s") }
        rows.drop(1).forEachIndexed { index,row -> if(row.any{it.isNotBlank()}) try {
            require(row.size==headers.size){"列数错误"};require(row[1].isNotBlank()&&row[2].isNotBlank()){"名称和分类不能为空"};require(Regex("[0-9]{1,12}(\\.[0-9]{1,2})?").matches(row[5])){"金额格式错误"}
            val cents=BigDecimal(row[5]).movePointRight(2).longValueExact();require(cents>0){"金额必须大于零"};require(row[9] in listOf("使用中","闲置","已出售","已报废")){"状态无效"}
            val i=Item(name=row[1],category=row[2],brand=row[3],model=row[4],purchasePriceCent=cents,purchaseDate=date(row[6]),warrantyEndDate=row[7].takeIf{it.isNotBlank()}?.let(::date),note=row[8],status=row[9],createdAt=row[10].takeIf{it.isNotBlank()}?.toLong()?:System.currentTimeMillis(),updatedAt=row[11].takeIf{it.isNotBlank()}?.toLong()?:System.currentTimeMillis())
            if(seen.add(key(i)))items+=i else duplicates++
        }catch(e:Exception){errors+="第${index+2}条记录：${e.message}"} }
        return ItemCsvPreview(items,errors,duplicates)
    }
    private fun rows(s:String):List<List<String>> {
        val result=mutableListOf<List<String>>();val row=mutableListOf<String>();val field=StringBuilder();var quoted=false;var closed=false;var n=0
        fun endField(){row+=field.toString();field.clear();closed=false}
        fun endRow(){endField();result+=row.toList();row.clear()}
        while(n<s.length){val c=s[n];when {
            quoted -> if(c=='"'){if(s.getOrNull(n+1)=='"'){field.append('"');n++}else{quoted=false;closed=true}}else field.append(c)
            c=='"' -> {require(field.isEmpty()&&!closed){"引号位置错误"};quoted=true}
            c==',' -> endField()
            c=='\n'||c=='\r' -> {endRow();if(c=='\r'&&s.getOrNull(n+1)=='\n')n++}
            else -> {require(!closed){"引号后存在非法字符"};field.append(c)}
        };n++ }
        require(!quoted){"引号未闭合"};if(field.isNotEmpty()||row.isNotEmpty()||closed)endRow();return result
    }
}
