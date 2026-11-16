package com.myledger.app.ui.components.charts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
@Composable fun ExpenseLineChart(values:List<Long>,modifier:Modifier=Modifier){
 val color=MaterialTheme.colorScheme.primary;val grid=MaterialTheme.colorScheme.outlineVariant
 Canvas(modifier.fillMaxWidth().height(140.dp)){
 val padding=8.dp.toPx();val w=size.width-2*padding;val h=size.height-2*padding;val max=values.maxOrNull()?.toFloat()?.coerceAtLeast(1f)?:1f
 (0..3).forEach{i->val y=padding+h*i/3;drawLine(grid,Offset(padding,y),Offset(size.width-padding,y),1.dp.toPx())}
 if(values.isNotEmpty()){val step=w/maxOf(1,values.lastIndex);val line=Path();values.forEachIndexed{i,v->val x=padding+step*i;val y=padding+h-(v/max*h);if(i==0)line.moveTo(x,y)else line.lineTo(x,y)};val area=Path().apply{addPath(line);lineTo(padding+step*values.lastIndex,padding+h);lineTo(padding,padding+h);close()};drawPath(area,Brush.verticalGradient(listOf(color.copy(alpha=.16f),color.copy(alpha=.01f))));drawPath(line,color,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round));if(values.size==1)drawCircle(color,3.dp.toPx(),Offset(padding,padding+h-values.first()/max*h))}
 }
}
@Composable fun ExpenseDonutChart(parts:List<Pair<Long,Color>>,modifier:Modifier=Modifier){Canvas(modifier.fillMaxWidth().height(180.dp)){val stroke=18.dp.toPx();val diameter=size.minDimension-stroke*2;val start=Offset((size.width-diameter)/2,(size.height-diameter)/2);val total=parts.sumOf{it.first}.toFloat().coerceAtLeast(1f);var angle=-90f;parts.forEach{(v,c)->val sweep=v/total*360f;drawArc(c,angle+1f,(sweep-2f).coerceAtLeast(.1f),false,start,Size(diameter,diameter),style=Stroke(stroke,cap=StrokeCap.Butt));angle+=sweep}}}
