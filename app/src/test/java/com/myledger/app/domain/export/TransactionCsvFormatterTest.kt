package com.myledger.app.domain.export

import com.myledger.app.data.entity.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class TransactionCsvFormatterTest {
    @Test
    fun `exports readable Chinese values and cents as yuan`() {
        val csv = TransactionCsvFormatter.format(
            listOf(
                Transaction(
                    id = 42,
                    amountCent = 12345,
                    type = "支出",
                    category = "停车",
                    note = "商场地下车库",
                    date = 0
                )
            ),
            TimeZone.getTimeZone("UTC")
        )

        assertEquals(
            "\uFEFF记录ID,日期,类型,分类,金额（元）,备注\n42,1970-01-01 00:00,支出,停车,123.45,商场地下车库\n",
            csv
        )
    }

    @Test
    fun `quotes notes that contain csv delimiters`() {
        val csv = TransactionCsvFormatter.format(
            listOf(Transaction(amountCent = 1, type = "收入", category = "退款", note = "A, \"B\"\nC", date = 0)),
            TimeZone.getTimeZone("UTC")
        )

        assertEquals(
            "\uFEFF记录ID,日期,类型,分类,金额（元）,备注\n0,1970-01-01 00:00,收入,退款,0.01,\"A, \"\"B\"\"\nC\"\n",
            csv
        )
    }
}
