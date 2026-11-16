package com.myledger.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.BillPeriod
import com.myledger.app.domain.bill.shiftBillReference
import com.myledger.app.domain.statistics.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.viewmodel.LedgerData
import java.math.BigDecimal
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InsightStatistics(data: LedgerData, modifier: Modifier = Modifier, onTransaction: (Transaction) -> Unit = {}) {
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("收支统计", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("清楚知道，钱花在哪里", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IncomeExpenseAnalysis(data, Modifier.weight(1f), onTransaction)
    }
}

/** The entire panel exists only while expanded: no hidden week row or empty calendar space. */
@Composable
internal fun CalendarDatePanel(records: List<Transaction>, reference: Long, onDate: (Long) -> Unit) {
    val monthKey = ledgerDate(reference, "yyyy-MM")
    val calculated by produceState<LedgerMonth?>(null, records, monthKey) {
        value = withContext(Dispatchers.Default) { LedgerCalendar.month(records, reference) }
    }
    val month = calculated?.takeIf { "%04d-%02d".format(java.util.Locale.ROOT, it.year, it.month) == monthKey }
    val selectedDay = Calendar.getInstance().apply { timeInMillis = reference }.get(Calendar.DAY_OF_MONTH)
    Surface(Modifier.testTag("expanded-calendar"), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("一","二","三","四","五","六","日").forEach { label ->
                    Box(Modifier.weight(1f),contentAlignment=Alignment.Center) { Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if(month==null) Box(Modifier.fillMaxWidth().height(300.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp) }
            else {
                val slots=List<LedgerDay?>(month.leadingDays){null}+month.days
                val maximum=month.days.maxOf{it.expenseCent}.coerceAtLeast(1)
                Column {
                    for(row in 0 until (slots.size+6)/7) Row(Modifier.fillMaxWidth()) {
                        for(column in 0..6) {
                            val cell=slots.getOrNull(row*7+column)
                            if(cell==null) Spacer(Modifier.weight(1f).height(52.dp))
                            else DayCell(cell,month,cell.day==selectedDay,maximum,Modifier.weight(1f)){onDate(cell.timestamp)}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LedgerDay, month: LedgerMonth, selected: Boolean, maxExpense: Long, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shade = when {
        selected -> colors.onSurface
        day.expenseCent > 0 -> colors.error.copy(alpha = .07f + .16f * day.expenseCent.toFloat() / maxExpense)
        day.incomeCent > 0 -> colors.primaryContainer
        else -> Color.Transparent
    }
    val fontScale = LocalDensity.current.fontScale
    val diameter = maxOf(28f, 14f * fontScale + 10f)
    val rowHeight = diameter + 20f * fontScale + 4f
    val paint = remember { android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER } }
    fun compact(value: Long) = BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString()
    // A single drawing node replaces three text layouts per cell. Full amounts remain accessible.
    Canvas(modifier.height(rowHeight.dp).testTag("day-${month.year}-${month.month}-${day.day}")
        .clickable(onClick = onClick).semantics {
            contentDescription = "查看${month.year}年${month.month}月${day.day}日流水，收入${ledgerMoney(day.incomeCent)}元，支出${ledgerMoney(day.expenseCent)}元"
            this.selected = selected
        }) {
        val d = diameter.dp.toPx()
        drawRoundRect(shade, Offset((size.width - d) / 2, 1.dp.toPx()), Size(d, d), CornerRadius(11.dp.toPx()))
        fun label(text: String, fontSize: Float, centerY: Float, color: Color) {
            paint.textSize = fontSize.sp.toPx()
            paint.color = color.toArgb()
            val fitted = android.text.TextUtils.ellipsize(text, paint, (size.width-2.dp.toPx()).coerceAtLeast(1f), android.text.TextUtils.TruncateAt.END).toString()
            val baseline = centerY - (paint.ascent() + paint.descent()) / 2
            drawContext.canvas.nativeCanvas.drawText(fitted, size.width / 2, baseline, paint)
        }
        label(day.day.toString(), 14f, 1.dp.toPx() + d / 2, if(selected)colors.surface else colors.onSurface)
        if(day.expenseCent>0)label("−"+compact(day.expenseCent),8f,d+6.sp.toPx(),colors.error)
        if(day.incomeCent>0)label("+"+compact(day.incomeCent),8f,d+16.sp.toPx(),colors.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MonthSelector(reference: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    val current = Calendar.getInstance().apply { timeInMillis = reference }
    var year by remember { mutableIntStateOf(current.get(Calendar.YEAR)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择月份", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { onSelect(System.currentTimeMillis()) }) { Text("本月") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ActionIcon("left", "上一年") { if (year > 1900) year-- }
                Text("${year}年", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                ActionIcon("right", "下一年") { if (year < 2199) year++ }
            }
            for (row in 0..3) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 1..3) {
                    val month = row * 3 + column
                    FilledTonalButton(onClick = { onSelect(LedgerCalendar.at(year, month)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("${month}月") }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("取消") }
        }
    }
}
