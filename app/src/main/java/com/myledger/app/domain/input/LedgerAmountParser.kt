package com.myledger.app.domain.input

import java.math.BigDecimal

/** Reads money only; dates, times, phone numbers and quantities are not prices. */
internal data class MoneyMention(val range:IntRange,val amountCent:Long?)

internal object LedgerAmountParser {
 private const val number="[0-9零〇一二两三四五六七八九十百千万亿]+(?:[.点][0-9零〇一二两三四五六七八九]+)?"
 private val explicit=Regex("($number)\\s*(元|块钱|块)(?:\\s*([0-9零〇一二两三四五六七八九])(?![0-9零〇一二两三四五六七八九])(?:角|毛)?(?:钱)?)?")
 private val digits=mapOf('零' to 0,'〇' to 0,'一' to 1,'二' to 2,'两' to 2,'三' to 3,'四' to 4,'五' to 5,'六' to 6,'七' to 7,'八' to 8,'九' to 9)
 private fun whole(value:String):Long {
  if(value.all{it.isDigit()})return value.toLong()
  if(value.none{it in "十百千万亿"})return value.map{digits[it]?:error("Invalid number")}.joinToString("").toLong()
  var total=0L;var section=0L;var current=0L
  value.forEach{c->when(c){
   '十','百','千'->{val unit=when(c){'十'->10;'百'->100;else->1000};section+=(if(current==0L)1 else current)*unit;current=0}
   '万'->{section=(section+current)*10000;total+=section;section=0;current=0}
   '亿'->{total=(total+section+current)*100000000;section=0;current=0}
   else->current=(digits[c]?:error("Invalid number")).toLong()
  }}
  return total+section+current
 }
 private fun cents(value:String,fraction:String=""):Long?=runCatching{
  val parts=value.replace('点','.').split('.')
  val decimal=if(parts.size==2)parts[1].map{if(it.isDigit())it else ('0'.code+(digits[it]?:error("Invalid decimal"))).toChar()}.joinToString("")else ""
  require(decimal.length<=2)
  val money=BigDecimal(whole(parts[0])).add(if(decimal.isEmpty())BigDecimal.ZERO else BigDecimal("0.$decimal"))
   .add(if(fraction.isBlank())BigDecimal.ZERO else BigDecimal(whole(fraction)).movePointLeft(1))
  money.movePointRight(2).longValueExact().also{require(it in 1..99999999999999L)}
 }.getOrNull()
 fun find(text:String):List<Long?> = mentions(text).map{it.amountCent}
 fun mentions(text:String):List<MoneyMention> {
  val explicitMatches=explicit.findAll(text).toList()
  val explicitValues=explicitMatches.map{m->MoneyMention(m.range,if(text.substring(0,m.range.first).trimEnd().endsWithAny("-","负")||Regex("^[0-9零〇一二两三四五六七八九十百千万亿.点]").containsMatchIn(text.substring(m.range.last+1)))null else cents(m.groupValues[1],m.groupValues[3]))}
  val extra=Regex(number).findAll(text).filter{m->
   if(explicitMatches.any{m.range.first in it.range})return@filter false
   val before=text.substring(0,m.range.first).trimEnd()
   val after=text.substring(m.range.last+1).trimStart()
   !after.startsWithAny("年","月","日","号","点","时","分","秒","个","件","杯","份","次","共","笔","下","起","顿","餐","公里","千米","米","%",":","：")&&
    !before.endsWithAny(":","：","月","年","电话","手机","周","星期")
  }.map{m->val prefix=text.substring(0,m.range.first).trimEnd();MoneyMention(m.range,if(m.value.length>6||prefix.endsWithAny("-","负"))null else cents(m.value))}.toList()
  return (explicitValues+extra).sortedBy{it.range.first}
 }
 private fun String.startsWithAny(vararg values:String)=values.any{startsWith(it)}
 private fun String.endsWithAny(vararg values:String)=values.any{endsWith(it)}
}
