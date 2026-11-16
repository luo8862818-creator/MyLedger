package com.myledger.app.ui.components

import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/** Localizes just the calendar, without changing device language or global Locale defaults. */
@Composable
private fun ChineseCalendarLocale(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val current = LocalConfiguration.current
    val configuration = remember(current) { Configuration(current).apply { setLocale(Locale.SIMPLIFIED_CHINESE) } }
    val localized = remember(context, configuration) { context.createConfigurationContext(configuration) }
    CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration, LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChineseDatePickerDialog(initialSelectedDateMillis: Long, onDismiss: () -> Unit, onSelected: (Long) -> Unit) {
    ChineseCalendarLocale {
        val state = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let(onSelected); onDismiss() }) { Text("确定") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        ) {
            ChineseCalendarLocale { DatePicker(state = state) }
        }
    }
}
