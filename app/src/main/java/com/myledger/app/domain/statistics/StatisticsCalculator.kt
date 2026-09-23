package com.myledger.app.domain.statistics

import com.myledger.app.data.entity.Transaction

data class PeriodSummary(val incomeCent: Long, val expenseCent: Long) { val balanceCent get() = incomeCent - expenseCent }
data class CategoryTotal(val category: String, val amountCent: Long, val ratio: Float)

object StatisticsCalculator {
    fun summary(transactions: List<Transaction>) = PeriodSummary(
        transactions.filter { it.type == "收入" }.sumOf { it.amountCent },
        transactions.filter { it.type == "支出" }.sumOf { it.amountCent }
    )
    fun categories(transactions: List<Transaction>): List<CategoryTotal> {
        val values = transactions.filter { it.type == "支出" }.groupBy { it.category }.mapValues { it.value.sumOf(Transaction::amountCent) }
        val total = values.values.sum()
        return values.map { CategoryTotal(it.key, it.value, if (total == 0L) 0f else it.value.toFloat() / total) }.sortedByDescending { it.amountCent }
    }
    fun comparison(current: Long, previous: Long): Float? = if (previous == 0L) null else (current - previous).toFloat() / previous
}
