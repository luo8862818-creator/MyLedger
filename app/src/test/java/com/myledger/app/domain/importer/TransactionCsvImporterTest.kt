package com.myledger.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class TransactionCsvImporterTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `imports current backup including ids and quoted notes`() {
        val result = TransactionCsvImporter.parse(
            "\uFEFF记录ID,日期,类型,分类,金额（元）,备注\n42,1970-01-01 00:00,支出,停车,123.45,\"A, \"\"B\"\"\nC\"\n",
            utc
        )

        assertEquals(0, result.invalidRows)
        assertEquals(1, result.transactions.size)
        assertEquals(42, result.transactions.single().id)
        assertEquals(12345, result.transactions.single().amountCent)
        assertEquals("A, \"B\"\nC", result.transactions.single().note)
    }

    @Test
    fun `accepts old backups and skips malformed rows`() {
        val result = TransactionCsvImporter.parse(
            "日期,类型,分类,金额（元）,备注\n1970-01-01 00:00,收入,工资,10.00,旧备份\ninvalid,支出,餐饮,abc,坏数据\n",
            utc
        )

        assertEquals(1, result.transactions.size)
        assertEquals(0, result.transactions.single().id)
        assertEquals(1, result.invalidRows)
    }
}
