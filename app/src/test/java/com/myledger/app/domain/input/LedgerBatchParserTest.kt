package com.myledger.app.domain.input

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class LedgerBatchParserTest {
 @Test fun delimitersAndCompactGrammar(){
  listOf("吃饭20，打车30","吃饭20,打车30","吃饭20；打车30","吃饭20\n打车30","吃饭20打车30","吃饭20元然后打车30元","吃饭20和打车30","吃饭二十元，打车三十元","吃饭20打车30元","吃饭20元打车30").forEach{input->
   val b=LedgerBatchParser.parse(input);assertTrue(input+":"+b.problems,b.readyToSave);assertEquals(input,listOf(2000L,3000L),b.entries.map{it.amountCent});assertEquals(listOf("餐饮","出行"),b.entries.map{it.category});assertEquals(5000L,b.expenseCent)
  }
 }
 @Test fun moneyBeforePurposeRequiresClearClause(){
  assertTrue(LedgerBatchParser.parse("20元吃饭，30元打车").readyToSave)
  assertFalse(LedgerBatchParser.parse("20元吃饭30元打车").readyToSave)
 }
 @Test fun amountPrecisionAndNormalization(){
  val b=LedgerBatchParser.parse("早餐１２.５０元，打车三十块五")
  assertTrue(b.problems.toString(),b.readyToSave);assertEquals(listOf(1250L,3050L),b.entries.map{it.amountCent});assertEquals(4300L,b.expenseCent)
 }
 @Test fun noDeduplicationOfLegitimatePurchases(){val b=LedgerBatchParser.parse("吃饭20，吃饭20");assertTrue(b.readyToSave);assertEquals(2,b.entries.size);assertEquals(4000L,b.expenseCent)}
 @Test fun oneUnclearClauseBlocksEntireBatch(){
  listOf("吃饭20，打车","吃饭20，转账30元","吃饭20，打车-30元","吃饭20，买东西","吃饭和打车一共50","吃饭20，打车30，不是40元","吃饭20，15元优惠后10元").forEach{assertFalse(it,LedgerBatchParser.parse(it).readyToSave)}
 }
 @Test fun totalsAreChecksumsNeverBills(){
  listOf("吃饭20，打车30，一共50","吃饭20打车30合计50","吃饭20元打车30元总共50").forEach{val b=LedgerBatchParser.parse(it);assertTrue(it+":"+b.problems,b.readyToSave);assertEquals(2,b.entries.size)}
  listOf("吃饭20，打车30，合计60","吃饭20元打车30元总共60","工资8000，吃饭20，合计8020元").forEach{assertFalse(it,LedgerBatchParser.parse(it).readyToSave)}
 }
 @Test fun incomeAndExpenseRemainSeparate(){val b=LedgerBatchParser.parse("工资8000，吃饭20，打车30");assertTrue(b.readyToSave);assertEquals(800000L,b.incomeCent);assertEquals(5000L,b.expenseCent);assertEquals(listOf("收入","支出","支出"),b.entries.map{it.type})}
 @Test fun relativeDateScopeAndOverrides(){
  val now=Calendar.getInstance().apply{set(2026,10,16,12,0,0)}.timeInMillis
  val b=LedgerBatchParser.parse("昨天，吃饭20，打车30，今天加油200",now)
  assertTrue(b.readyToSave)
  assertEquals(listOf(15,15,16),b.entries.map{Calendar.getInstance().apply{timeInMillis=it.date!!}.get(Calendar.DAY_OF_MONTH)})
 }
 @Test fun limitsAndEmptyInput(){
  assertFalse(LedgerBatchParser.parse("").readyToSave);assertFalse(LedgerBatchParser.parse("今天").readyToSave)
  assertTrue(LedgerBatchParser.parse(List(20){"吃饭20"}.joinToString("，")).readyToSave)
  assertFalse(LedgerBatchParser.parse(List(21){"吃饭20"}.joinToString("，")).readyToSave)
  assertFalse(LedgerBatchParser.parse("吃饭20".repeat(600)).readyToSave)
 }
 @Test fun customCategoryRulesAreSharedWithScalarParser(){val b=LedgerBatchParser.parse("支付小店20，打车30",customExpense=mapOf("小店消费" to listOf("小店")));assertTrue(b.readyToSave);assertEquals("小店消费",b.entries.first().category)}
 @Test fun categoryLabelsUseTheSameLexiconAsAliases(){val b=LedgerBatchParser.parse("餐饮20，出行30，购物40");assertTrue(b.problems.toString(),b.readyToSave);assertEquals(listOf("餐饮","出行","购物"),b.entries.map{it.category})}

 @Test fun alternativesNegationAndPlansNeverBecomeActualBills(){
  listOf("吃饭20或者打车30","吃饭20或打车30","吃饭20，没有打车30","吃饭20，预算打车30","明天吃饭20，打车30","吃饭20，大概打车30").forEach{assertFalse(it,LedgerBatchParser.parse(it).readyToSave)}
 }

}
