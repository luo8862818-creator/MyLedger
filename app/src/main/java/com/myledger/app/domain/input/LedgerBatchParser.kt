package com.myledger.app.domain.input

import java.text.Normalizer

data class ParsedLedgerBatch(val entries:List<ParsedLedgerInput>,val problems:List<String>){
 val readyToSave get()=entries.isNotEmpty()&&problems.isEmpty()&&entries.none{it.needsConfirmation}
 val incomeCent get()=entries.filter{it.type=="收入"}.sumOf{it.amountCent?:0}
 val expenseCent get()=entries.filter{it.type=="支出"}.sumOf{it.amountCent?:0}
}

/**
 * Pipeline: normalize -> explicit clauses -> money-span boundaries -> scalar parser -> validate.
 * Money spans come from the same scanner used by scalar parsing. No second money regex in the UI.
 * A batch is indivisible: an incomplete clause, ambiguous allocation or inconsistent total blocks all.
 */
object LedgerBatchParser {
 const val MAX_ENTRIES=20
 private val clauses=Regex("[,，;；。\n]+")
 private val connector=Regex("""^(?:(?:然后|接着|再|另外|还有|以及|并且|和|与)\s*)+""")
 private val datePrefix=Regex("^(今天|昨天|前天)")
 private val alternative=Regex("或者|还是|要么|或")
 private val summary=Regex("^(?:一共|总共|合计|总计|共计|总额)")
 fun parse(raw:String,now:Long=System.currentTimeMillis(),customExpense:Map<String,List<String>> = emptyMap(),customIncome:Map<String,List<String>> = emptyMap()):ParsedLedgerBatch {
  if(raw.length>2000)return ParsedLedgerBatch(emptyList(),listOf("描述过长，请每次最多输入 $MAX_ENTRIES 笔"))
  val text=Normalizer.normalize(raw.trim(),Normalizer.Form.NFKC)
  if(text.isBlank())return ParsedLedgerBatch(emptyList(),listOf("请描述这笔收支"))
  if(alternative.containsMatchIn(text))return ParsedLedgerBatch(emptyList(),listOf("包含备选项，无法确定实际发生的账单，请修改描述"))
  val segments=clauses.split(text).map{it.trim()}.filter{it.isNotEmpty()}.flatMap(::splitCompact)
  if(segments.size>MAX_ENTRIES+1)return ParsedLedgerBatch(emptyList(),listOf("每次最多保存 $MAX_ENTRIES 笔，请分批描述"))
  val entries=mutableListOf<ParsedLedgerInput>();val problems=mutableListOf<String>();val totals=mutableListOf<Long?>()
  var dateContext=""
  for(segment in segments){
   val clause=clean(segment)
   val explicitDate=datePrefix.find(clause)?.value
   if(explicitDate!=null)dateContext=explicitDate
   if(clause in listOf("今天","昨天","前天"))continue
   if(summary.containsMatchIn(clause)){
    // A stated total is a checksum, never an extra transaction.
    val values=LedgerAmountParser.find("支付"+clause)
    totals+=values.singleOrNull()
    continue
   }
   val input=if(explicitDate==null&&dateContext.isNotEmpty())dateContext+clause else clause
   val parsed=LedgerInputParser.parse(input,now,customExpense=customExpense,customIncome=customIncome).copy(originalText=clause,note=clause)
   entries+=parsed
   if(parsed.needsConfirmation)problems+="第${entries.size}笔："+parsed.warnings.joinToString("；")
  }
  if(entries.isEmpty())problems+="未识别到具体账单"
  if(entries.size>MAX_ENTRIES)problems+="每次最多保存 $MAX_ENTRIES 笔"
  if(totals.isNotEmpty()){
   val unambiguous=entries.isNotEmpty()&&entries.none{it.needsConfirmation}&&entries.map{it.type}.distinct().size==1
   if(totals.size!=1||totals.single()==null||!unambiguous||totals.single()!=entries.sumOf{it.amountCent?:0})problems+="合计与各笔金额不一致，或收支混合，请修改描述"
  }
  return ParsedLedgerBatch(entries,problems.distinct())
 }
 private fun clean(value:String)=value.trim().replaceFirst(connector,"").trim()
 private fun splitCompact(clause:String):List<String>{
  val mentions=LedgerAmountParser.mentions(clause)
  if(mentions.size<=1)return listOf(clause)
  // Only split compact description-then-price grammar. Price-first allocation without delimiters
  // remains ambiguous and is sent to scalar validation instead of guessing where a purpose belongs.
  if(clause.substring(0,mentions.first().range.first).isBlank())return listOf(clause)
  var start=0
  return mentions.mapIndexed{index,m->
   val end=if(index==mentions.lastIndex)clause.length else m.range.last+1
   clause.substring(start,end).also{start=end}
  }
 }
}
