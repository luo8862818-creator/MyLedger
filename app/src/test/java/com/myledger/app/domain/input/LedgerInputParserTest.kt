package com.myledger.app.domain.input
import org.junit.Assert.*
import org.junit.Test
class LedgerInputParserTest {
 @Test fun fixedSamples(){
  listOf(Triple("吃饭花了15元","餐饮",1500L),Triple("中午外卖28.5元","餐饮",2850L),Triple("打车花了28块","出行",2800L),Triple("加油200元","加油",20000L),Triple("停车费10元","停车",1000L),Triple("买衣服花了199元","购物",19900L),Triple("工资到账8000元","工资",800000L),Triple("收到奖金2000元","奖金",200000L)).forEach{(text,category,amount)->val p=LedgerInputParser.parse(text);assertEquals(text,category,p.category);assertEquals(text,amount,p.amountCent);assertEquals(if(category in listOf("工资","奖金"))"收入" else "支出",p.type)}
 }
 @Test fun ambiguityAndNegation(){listOf("吃饭和打车一共50元","买衣服和吃饭共80元","今天没花钱","不是餐饮花了15元").forEach{assertNull(LedgerInputParser.parse(it).category)};assertNull(LedgerInputParser.parse("今天没花钱").amountCent);assertEquals("餐饮",LedgerInputParser.parse("咖啡22元").category);assertNull(LedgerInputParser.parse("买菜").amountCent);assertNull(LedgerInputParser.parse("吃饭15元打车20元").amountCent)}
 @Test fun invalidAmounts(){listOf("吃饭0元","吃饭1.234元","吃饭99999999999999999999999元").forEach{assertNull(it,LedgerInputParser.parse(it).amountCent)}}
 @Test fun normalizedAndHistory(){assertEquals(1500L,LedgerInputParser.parse("吃饭１５元").amountCent);val now=System.currentTimeMillis();assertTrue(LedgerInputParser.parse("昨天吃饭15元",now).date!!<now)}
 @Test fun everydayUnitlessAndSpokenMoney(){
  listOf("吃饭20" to 2000L,"吃饭一共20" to 2000L,"周一吃饭20" to 2000L,"收到一笔工资8000" to 800000L,"午饭二十五元" to 2500L,"早餐12.5" to 1250L,"停车十块" to 1000L,"吃饭二十块五" to 2050L,"打车三十二点五元" to 3250L,"工资8000" to 800000L,"昨天11月16日吃饭20" to 2000L).forEach{(text,cents)->val p=LedgerInputParser.parse(text);assertEquals(text,cents,p.amountCent);assertFalse(text,p.needsConfirmation)}
 }
 @Test fun incompleteAndMultipleAmountsCannotAutoSave(){
  listOf("吃饭","吃饭20打车30","昨天11月16日吃饭","打车08:30","今天没花钱","吃饭20元打车30元","吃饭20元打车30","吃饭20打车30元","吃饭-20元","吃饭20元2026","吃饭13800138000","转账20元").forEach{assertTrue(it,LedgerInputParser.parse(it).needsConfirmation)}
 }
 @Test fun customRulesRespectDirection(){val p=LedgerInputParser.parse("支付小店15元",customExpense=mapOf("自定义" to listOf("小店")));assertEquals("自定义",p.category);assertNull(LedgerInputParser.parse("工资到账8000元",customExpense=mapOf("错误分类" to listOf("工资"))).candidates.find{it.category=="错误分类"})}
}
