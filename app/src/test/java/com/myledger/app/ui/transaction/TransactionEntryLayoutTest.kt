package com.myledger.app.ui.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionEntryLayoutTest {
    @Test
    fun collapsedSheetUsesItsNormalHeightWhenKeyboardIsHidden() {
        val dimensions = TransactionEntryLayout.calculate(
            viewportHeightPx = 2400,
            imeBottomPx = 0,
            navigationBottomPx = 100,
            expanded = false
        )

        assertEquals(100, dimensions.bottomInsetPx)
        assertEquals(1344, dimensions.sheetHeightPx)
    }

    @Test
    fun expandedSheetNeverExtendsBehindTheKeyboard() {
        val dimensions = TransactionEntryLayout.calculate(
            viewportHeightPx = 2400,
            imeBottomPx = 920,
            navigationBottomPx = 100,
            expanded = true
        )

        assertEquals(920, dimensions.bottomInsetPx)
        assertEquals(1480, dimensions.sheetHeightPx)
        assertTrue(dimensions.sheetHeightPx + dimensions.bottomInsetPx <= 2400)
    }

    @Test
    fun navigationInsetIsUsedWhenItIsLargerThanTheKeyboardInset() {
        val dimensions = TransactionEntryLayout.calculate(
            viewportHeightPx = 1000,
            imeBottomPx = 0,
            navigationBottomPx = 120,
            expanded = true
        )

        assertEquals(120, dimensions.bottomInsetPx)
        assertEquals(840, dimensions.sheetHeightPx)
    }

    @Test
    fun extremeKeyboardInsetsStillProduceAValidVisibleSheet() {
        val dimensions = TransactionEntryLayout.calculate(
            viewportHeightPx = 3200,
            imeBottomPx = 3199,
            navigationBottomPx = 120,
            expanded = true
        )

        assertEquals(3199, dimensions.bottomInsetPx)
        assertEquals(1, dimensions.sheetHeightPx)
        assertTrue(dimensions.sheetHeightPx + dimensions.bottomInsetPx <= 3200)
    }
}
