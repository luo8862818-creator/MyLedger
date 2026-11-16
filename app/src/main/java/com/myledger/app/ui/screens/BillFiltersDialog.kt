package com.myledger.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myledger.app.domain.bill.*
import com.myledger.app.ui.components.*
import com.myledger.app.ui.transaction.CategoryRules

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BillFiltersDialog(
    period: BillPeriod,
    type: String,
    query: String,
    category: String,
    referenceDate: Long,
    onApply: (BillPeriod, String, String, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var p by remember { mutableStateOf(period) };
    var t by remember { mutableStateOf(type) };
    var q by remember { mutableStateOf(query) };
    var c by remember { mutableStateOf(category) };
    var d by remember { mutableLongStateOf(referenceDate) };
    var pick by remember { mutableStateOf(false) }
    val context = LocalContext.current;
    val rules = remember { CategoryRules(context) }
    val expense = listOf(
        "餐饮",
        "出行",
        "加油",
        "停车",
        "购物",
        "居住",
        "娱乐",
        "其他"
    ) + rules.read("支出").keys;
    val income = listOf("工资", "奖金", "兼职", "退款", "其他收入") + rules.read("收入").keys
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier
         .fillMaxWidth()
         .fillMaxHeight(.9f)
         .imePadding()) {
            Row(
                Modifier
                 .fillMaxWidth()
                 .padding(horizontal = 22.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "筛选账单",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                ); TextButton(onClick = {
                p = BillPeriod.MONTH; t = ""; q = ""; c = ""; d = System.currentTimeMillis()
            }) { Text("重置") }
            }
            Column(
                Modifier
                 .weight(1f)
                 .verticalScroll(rememberScrollState())
                 .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "时间范围",
                        style = MaterialTheme.typography.titleMedium
                    ); SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    BillPeriod.entries.forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = p == v,
                            onClick = { p = v },
                            shape = SegmentedButtonDefaults.itemShape(i, 4),
                            icon = {},
                            label = { Text(v.label) })
                    }
                }; SettingsRow(
                    "calendar",
                    "当前周期",
                    billPeriodTitle(p, d),
                    { pick = true }); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "本月",
                        "上月",
                        "今年"
                    ).forEach { label ->
                        SuggestionChip(onClick = {
                            d = System.currentTimeMillis(); p =
                            if (label == "今年") BillPeriod.YEAR else BillPeriod.MONTH; if (label == "上月") d =
                            shiftBillReference(p, d, -1)
                        }, label = { Text(label) })
                    }
                }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "收支类型",
                        style = MaterialTheme.typography.titleMedium
                    ); SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(
                        "",
                        "支出",
                        "收入"
                    ).forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = t == v,
                            onClick = { t = v; c = "" },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                            icon = {},
                            label = { Text(v.ifBlank { "全部" }) })
                    }
                }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "分类",
                        style = MaterialTheme.typography.titleMedium
                    ); FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (listOf("") + when (t) {
                        "收入" -> income; "支出" -> expense; else -> expense + income
                    }).distinct().forEach { v ->
                        FilterChip(
                            selected = c == v,
                            onClick = { c = v },
                            label = { Text(v.ifBlank { "全部分类" }) })
                    }
                }
                }
                OutlinedTextField(
                    q,
                    { q = it },
                    label = { Text("关键词") },
                    placeholder = { Text("搜索分类或备注") },
                    trailingIcon = {
                        if (q.isNotBlank()) IconButton(onClick = {
                            q = ""
                        }) { LedgerIcon("close") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Row(
                Modifier
                 .fillMaxWidth()
                 .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                     .weight(1f)
                     .heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("取消") }; Button(
                onClick = { onApply(p, t, q, c, d); onDismiss() },
                modifier = Modifier
                 .weight(2f)
                 .heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("确认筛选") }
            }
        }
    }
    if (pick) {
        ChineseDatePickerDialog(initialSelectedDateMillis = d, onDismiss = { pick = false }, onSelected = { d = it })
    }
}
