package com.myledger.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myledger.app.data.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntrySheet(isSaving: Boolean, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("支出") }
    var category by remember { mutableStateOf("餐饮") }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val categories = if (type == "支出") {
        listOf("餐饮", "出行", "加油", "停车", "购物", "居住", "娱乐", "其他")
    } else {
        listOf("工资", "奖金", "兼职", "退款", "其他收入")
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val amountRequester = remember { BringIntoViewRequester() }
    val timeRequester = remember { BringIntoViewRequester() }
    val noteRequester = remember { BringIntoViewRequester() }

    fun revealField(requester: BringIntoViewRequester) {
        expanded = true
        coroutineScope.launch {
            // Let the keyboard and the resized sheet settle before revealing the focused field.
            delay(80)
            requester.bringIntoView()
        }
    }

    fun submit() {
        val cents = parseCents(amount)
        val timestamp = timestampFor(date, time)
        when {
            cents == null -> error = "请输入有效金额（最多两位小数）"
            timestamp == null -> error = "时间格式请使用 24 小时制，例如 08:30"
            else -> onSave(
                Transaction(
                    amountCent = cents,
                    type = type,
                    category = category,
                    note = note.trim(),
                    date = timestamp
                )
            )
        }
    }

    // A static surface avoids the expensive enter animation of ModalBottomSheet on lower-end devices.
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val layout = TransactionEntryLayout.calculate(
                viewportHeightPx = with(density) { maxHeight.roundToPx() },
                imeBottomPx = WindowInsets.ime.getBottom(density),
                navigationBottomPx = WindowInsets.navigationBars.getBottom(density),
                expanded = expanded
            )
            val sheetHeight = with(density) { layout.sheetHeightPx.toDp() }
            val bottomInset = with(density) { layout.bottomInsetPx.toDp() }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset)
                    .fillMaxWidth()
                    .height(sheetHeight),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 6.dp
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 24.dp)
                    ) {
                    EntrySheetHandle(
                        expanded = expanded,
                        onExpand = { expanded = true },
                        onCollapse = { expanded = false }
                    )
                    Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (expanded) "下拉顶部横条可收起" else "向上拉顶部横条可展开全部内容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it; error = "" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(amountRequester)
                                .onFocusChanged { if (it.isFocused) revealField(amountRequester) },
                            label = { Text("金额") },
                            prefix = { Text("¥", style = MaterialTheme.typography.headlineMedium) },
                            textStyle = MaterialTheme.typography.headlineMedium,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("支出", "收入").forEach { option ->
                                FilterChip(
                                    selected = type == option,
                                    onClick = {
                                        type = option
                                        category = if (option == "支出") "餐饮" else "工资"
                                    },
                                    label = { Text(option, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                        Text("分类", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(option, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(Date(date)),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("日期") },
                            trailingIcon = {
                                TextButton(onClick = {
                                    focusManager.clearFocus(force = true)
                                    showDatePicker = true
                                }) { Text("选择") }
                            }
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it; error = "" },
                            modifier = Modifier
                                .width(160.dp)
                                .bringIntoViewRequester(timeRequester)
                                .onFocusChanged { if (it.isFocused) revealField(timeRequester) },
                            label = { Text("时间") },
                            placeholder = { Text("08:30") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(noteRequester)
                                .onFocusChanged { if (it.isFocused) revealField(noteRequester) },
                            label = { Text("备注（可选）") },
                            maxLines = 2
                        )
                        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(enabled = !isSaving, onClick = onDismiss) { Text("取消") }
                        Button(
                            enabled = !isSaving,
                            onClick = ::submit,
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text(if (isSaving) "保存中" else "确定") }
                    }
                }
                }
            }
        }
    }
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun EntrySheetHandle(expanded: Boolean, onExpand: () -> Unit, onCollapse: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(expanded) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        when {
                            totalDrag < -24f -> onExpand()
                            totalDrag > 24f -> onCollapse()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
        )
    }
}

private fun parseCents(raw: String): Long? {
    if (!Regex("^\\d{1,12}(\\.\\d{1,2})?$").matches(raw)) return null
    val parts = raw.split('.')
    return try {
        Math.addExact(
            Math.multiplyExact(parts[0].toLong(), 100),
            parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0
        ).takeIf { it > 0 }
    } catch (_: Exception) {
        null
    }
}

private fun timestampFor(date: Long, rawTime: String): Long? {
    val match = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$").matchEntire(rawTime) ?: return null
    return Calendar.getInstance().apply {
        timeInMillis = date
        set(Calendar.HOUR_OF_DAY, match.groupValues[1].toInt())
        set(Calendar.MINUTE, match.groupValues[2].toInt())
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
