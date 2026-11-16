package com.myledger.app.domain.input

import java.text.Normalizer
import java.util.Calendar

data class CategoryCandidate(val category: String, val score: Int, val matchedTerms: List<String>)
data class ParsedLedgerInput(val originalText: String, val amountCent: Long?, val type: String?, val category: String?, val note: String, val date: Long?, val confidence: Float, val candidates: List<CategoryCandidate>, val needsConfirmation: Boolean, val warnings: List<String>)
object LedgerInputParser {
    private val incomeIntent=Regex("工资|奖金|薪水|收到|收款|到账|收入|退款")
    private val negation=Regex("没花钱|不花钱|不是|(?:没有|没|未)(?:吃饭|打车|加油|停车|买|支付|付款|消费|花)")
    private val nonActualIntent=Regex("预算|打算|计划|预计|大概|可能|估计|准备|明天|后天")
    private val expenseIntent=Regex("花了|支付|付款|扣款|消费|买|支出")
    val expenseTerms = mapOf("餐饮" to listOf("吃饭", "早餐", "午餐", "午饭", "晚餐", "晚饭", "早饭", "外卖", "奶茶", "咖啡", "火锅"), "出行" to listOf("打车", "公交", "地铁", "车票"), "加油" to listOf("加油", "汽油"), "停车" to listOf("停车费", "停车"), "购物" to listOf("买衣服", "网购", "超市", "买菜"), "居住" to listOf("房租", "物业费"), "娱乐" to listOf("电影", "游戏", "演出"))
    val incomeTerms = mapOf("工资" to listOf("工资", "薪水", "月薪"), "奖金" to listOf("奖金", "绩效", "年终奖"), "兼职" to listOf("兼职", "劳务报酬"), "退款" to listOf("退款到账", "商家退款"))
    fun parse(raw: String, now: Long = System.currentTimeMillis(), custom: Map<String, List<String>> = emptyMap(), customExpense: Map<String,List<String>> = emptyMap(), customIncome: Map<String,List<String>> = emptyMap()): ParsedLedgerInput {
        val text = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
        val warnings = mutableListOf<String>()
        val negated = negation.containsMatchIn(text)
        if(nonActualIntent.containsMatchIn(text))warnings+="包含计划或估算，请确认实际发生后修改描述"
        val amounts = LedgerAmountParser.find(text)
        val amount = if(amounts.size==1&&!negated) amounts.single() else null
        if (amount == null) warnings += if(amounts.size > 1) "多个金额，请分别描述每一笔账" else "请补充有效金额"
        val expenseLexicon=expenseTerms+customExpense
        val incomeLexicon=incomeTerms+customIncome
        fun matches(lexicon:Map<String,List<String>>)=lexicon.any{(category,terms)->(terms+category).any{text.contains(it)}}
        val explicitIncome=incomeIntent.containsMatchIn(text)
        val explicitExpense=expenseIntent.containsMatchIn(text)
        val income=if(explicitIncome||explicitExpense)explicitIncome else matches(incomeLexicon)
        val expense=if(explicitIncome||explicitExpense)explicitExpense else matches(expenseLexicon)
        val type = when { negated || text.contains("转账") || (income && expense) -> null; income -> "收入"; expense -> "支出"; else -> null }
        val dictionary = when(type) { "收入" -> incomeLexicon; "支出" -> expenseLexicon; else -> expenseLexicon + incomeLexicon }
        val preferences = custom + when(type){"收入"->customIncome;"支出"->customExpense;else->emptyMap()}
        val candidates = dictionary.map { (category, terms) ->
            val matches = (preferences[category].orEmpty() + terms + category).distinct().filter { text.contains(it) }
            CategoryCandidate(category, if(negated) 0 else matches.sumOf { if(it in preferences[category].orEmpty()) 8 else if(it.length >= 3) 5 else 3 } + if(matches.isNotEmpty() && type != null) 1 else 0, matches)
        }.filter { it.score > 0 && it.matchedTerms.isNotEmpty() }.sortedByDescending { it.score }
        val best = candidates.firstOrNull()?.score ?: 0
        val second = candidates.getOrNull(1)?.score ?: 0
        val ambiguous = candidates.size > 1 && best-second < 5
        val confidence = if(best == 0) 0f else ((best-second).toFloat()/best).coerceIn(0f,1f)
        val category = candidates.firstOrNull()?.category?.takeIf { best >= 4 && !ambiguous && type != null && !negated }
        if(ambiguous) warnings += "涉及多个分类，请分别描述每一笔账"
        if(type == null) warnings += "请确认收支方向"
        if(category == null) warnings += "请补充具体用途或分类"
        val date = Calendar.getInstance().apply { timeInMillis=now; if(text.contains("昨天")) add(Calendar.DAY_OF_YEAR,-1); if(text.contains("前天")) add(Calendar.DAY_OF_YEAR,-2) }.timeInMillis
        return ParsedLedgerInput(raw, amount, type, category, raw.trim(), date, confidence, candidates, amount==null||type==null||category==null||warnings.isNotEmpty(), warnings)
    }
}
