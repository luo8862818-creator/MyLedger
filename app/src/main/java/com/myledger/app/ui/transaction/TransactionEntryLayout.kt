package com.myledger.app.ui.transaction

/**
 * Keeps the entry form above whichever system surface occupies the most space.
 * The arithmetic is platform-independent so keyboard edge cases can be unit tested.
 */
internal object TransactionEntryLayout {
    private const val COLLAPSED_FRACTION = 0.56f
    private const val EXPANDED_FRACTION = 0.84f

    data class Dimensions(val sheetHeightPx: Int, val bottomInsetPx: Int)

    fun calculate(
        viewportHeightPx: Int,
        imeBottomPx: Int,
        navigationBottomPx: Int,
        expanded: Boolean
    ): Dimensions {
        val viewport = viewportHeightPx.coerceAtLeast(1)
        val bottomInset = maxOf(imeBottomPx, navigationBottomPx, 0)
        val usableHeight = (viewport - bottomInset).coerceAtLeast(1)
        val fraction = if (expanded) EXPANDED_FRACTION else COLLAPSED_FRACTION
        val preferredHeight = (viewport * fraction).toInt().coerceAtLeast(1)
        return Dimensions(
            sheetHeightPx = minOf(preferredHeight, usableHeight),
            bottomInsetPx = bottomInset
        )
    }
}
