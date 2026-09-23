package com.myledger.app.domain.export

import com.myledger.app.data.entity.Transaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds a UTF-8 (Excel-friendly) CSV backup without depending on Android APIs. */
object TransactionCsvFormatter {
    private const val Header = "记录ID,日期,类型,分类,金额（元）,备注"

    fun format(
        transactions: List<Transaction>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            this.timeZone = timeZone
        }
        return buildString {
            // Excel on many Android/Windows devices still relies on the BOM to recognise UTF-8 Chinese text.
            append('\uFEFF')
            appendLine(Header)
            transactions.forEach { transaction ->
                append(transaction.id)
                append(',')
                appendCsvField(dateFormat.format(Date(transaction.date)))
                append(',')
                appendCsvField(transaction.type)
                append(',')
                appendCsvField(transaction.category)
                append(',')
                append(BigDecimal.valueOf(transaction.amountCent, 2).toPlainString())
                append(',')
                appendCsvField(transaction.note)
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendCsvField(value: String) {
        val requiresQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!requiresQuotes) {
            append(value)
            return
        }
        append('"')
        value.forEach { character ->
            if (character == '"') append("\"\"") else append(character)
        }
        append('"')
    }
}
