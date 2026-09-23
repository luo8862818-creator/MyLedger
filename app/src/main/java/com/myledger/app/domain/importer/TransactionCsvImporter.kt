package com.myledger.app.domain.importer

import com.myledger.app.data.entity.Transaction
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ParsedTransactionCsv(
    val transactions: List<Transaction>,
    val invalidRows: Int
)

/** Parses both current CSV backups and the previous five-column export format. */
object TransactionCsvImporter {
    private const val Id = "记录ID"
    private const val Date = "日期"
    private const val Type = "类型"
    private const val Category = "分类"
    private const val Amount = "金额（元）"
    private const val Note = "备注"
    private val requiredHeaders = setOf(Date, Type, Category, Amount, Note)

    fun parse(source: String, timeZone: TimeZone = TimeZone.getDefault()): ParsedTransactionCsv {
        val rows = parseRows(source)
        require(rows.isNotEmpty()) { "CSV 文件为空" }
        val headers = rows.first().map { it.removePrefix("\uFEFF").trim() }
        require(requiredHeaders.all { it in headers }) { "CSV 缺少必要列" }
        val headerIndex = headers.withIndex().associate { it.value to it.index }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            this.timeZone = timeZone
            isLenient = false
        }
        var invalidRows = 0
        val transactions = buildList {
            rows.drop(1).filter { row -> row.any { it.isNotBlank() } }.forEach { row ->
                try {
                    fun value(name: String): String = row.getOrNull(headerIndex.getValue(name)) ?: ""
                    val dateText = value(Date).trim()
                    val position = ParsePosition(0)
                    val parsedDate = dateFormat.parse(dateText, position)
                        ?.takeIf { position.index == dateText.length }
                        ?: error("日期格式错误")
                    val cents = BigDecimal(value(Amount).trim())
                        .movePointRight(2)
                        .longValueExact()
                        .takeIf { it > 0 }
                        ?: error("金额必须大于零")
                    val type = value(Type).trim().takeIf { it == "支出" || it == "收入" }
                        ?: error("收支类型错误")
                    val category = value(Category).trim().takeIf { it.isNotEmpty() }
                        ?: error("分类不能为空")
                    val id = headerIndex[Id]?.let { row.getOrNull(it)?.trim()?.toLongOrNull() }
                        ?.takeIf { it > 0 } ?: 0L
                    add(
                        Transaction(
                            id = id,
                            amountCent = cents,
                            type = type,
                            category = category,
                            note = value(Note),
                            date = parsedDate.time
                        )
                    )
                } catch (_: Exception) {
                    invalidRows++
                }
            }
        }
        return ParsedTransactionCsv(transactions, invalidRows)
    }

    private fun parseRows(source: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        fun finishField() {
            row += field.toString()
            field.clear()
        }
        fun finishRow() {
            finishField()
            rows += row.toList()
            row.clear()
        }
        while (index < source.length) {
            when (val character = source[index]) {
                '"' -> if (quoted && source.getOrNull(index + 1) == '"') {
                    field.append('"')
                    index++
                } else {
                    quoted = !quoted
                }
                ',' -> if (quoted) field.append(character) else finishField()
                '\n' -> if (quoted) field.append(character) else finishRow()
                '\r' -> if (quoted) field.append(character) else if (source.getOrNull(index + 1) != '\n') finishRow()
                else -> field.append(character)
            }
            index++
        }
        require(!quoted) { "CSV 引号未闭合" }
        if (field.isNotEmpty() || row.isNotEmpty()) finishRow()
        return rows
    }
}
