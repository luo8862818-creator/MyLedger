package com.myledger.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ExpenseLineChart(values: List<Long>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    Canvas(modifier.fillMaxWidth().height(150.dp)) {
        val max = values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val step = size.width / maxOf(1, values.lastIndex)
        for (index in 1 until values.size) {
            val a=Offset(step*(index-1), size.height-(values[index-1]/max*size.height)); val b=Offset(step*index, size.height-(values[index]/max*size.height))
            drawLine(Color(0xFF256B70), a, b, strokeWidth=6f, cap=StrokeCap.Round)
        }
    }
}

@Composable
fun ExpenseDonutChart(parts: List<Pair<Long, Color>>, modifier: Modifier = Modifier) {
    if (parts.isEmpty()) return
    Canvas(modifier.height(150.dp).fillMaxWidth()) {
        val total=parts.sumOf { it.first }.toFloat().coerceAtLeast(1f); var angle=-90f
        parts.forEach { (v,color) -> val sweep=v/total*360f; drawArc(color,angle,sweep,false,topLeft=Offset(size.width*.2f,0f),size=androidx.compose.ui.geometry.Size(size.height,size.height),style=Stroke(28f)); angle+=sweep }
    }
}
