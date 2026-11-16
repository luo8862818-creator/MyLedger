package com.myledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myledger.app.data.entity.Transaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

fun ledgerMoney(c:Long)=BigDecimal.valueOf(c,2).toPlainString()
fun ledgerDate(t:Long,pattern:String="MM月dd日 HH:mm")=SimpleDateFormat(pattern,Locale.CHINA).format(Date(t))
@Composable fun LedgerIcon(name:String,modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.onSurfaceVariant){
 Canvas(modifier.size(22.dp)){
  withTransform({scale(size.width/24f,size.height/24f,pivot=Offset.Zero)}){
   fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(color,Offset(x,y),Offset(a,b),1.7f,StrokeCap.Round)
   fun box(x:Float,y:Float,w:Float,h:Float,r:Float=3f)=drawRoundRect(color,Offset(x,y),Size(w,h),CornerRadius(r),style=Stroke(1.7f))
   when(name){
    "home"->{val p=Path().apply{moveTo(3f,10f);lineTo(12f,3f);lineTo(21f,10f);moveTo(5f,9f);lineTo(5f,21f);lineTo(10f,21f);lineTo(10f,14f);lineTo(14f,14f);lineTo(14f,21f);lineTo(19f,21f);lineTo(19f,9f)};drawPath(p,color,style=Stroke(1.7f,cap=StrokeCap.Round,join=StrokeJoin.Round))}
    "bills"->{box(5f,3f,14f,18f);line(9f,8f,15f,8f);line(9f,12f,15f,12f);line(9f,16f,13f,16f)}
    "items"->{box(3f,4f,18f,17f);line(3f,9f,21f,9f);line(10f,13f,14f,13f)}
    "stats"->{line(5f,20f,5f,12f);line(12f,20f,12f,4f);line(19f,20f,19f,8f)}
    "plus"->{line(12f,5f,12f,19f);line(5f,12f,19f,12f)}
    "close"->{line(6f,6f,18f,18f);line(18f,6f,6f,18f)}
    "search"->{drawCircle(color,6.5f,Offset(10.5f,10.5f),style=Stroke(1.7f));line(16f,16f,21f,21f)}
    "filter"->{line(4f,7f,20f,7f);line(7f,12f,17f,12f);line(10f,17f,14f,17f)}
    "mic"->{box(9f,2f,6f,13f);drawArc(color,0f,180f,false,Offset(5f,7f),Size(14f,12f),style=Stroke(1.7f));line(12f,19f,12f,22f);line(8f,22f,16f,22f)}
    "stop"->drawRoundRect(color,Offset(6f,6f),Size(12f,12f),CornerRadius(2f))
    "check"->{line(5f,12f,10f,17f);line(10f,17f,20f,6f)}
    "up"->{line(5f,15f,12f,8f);line(12f,8f,19f,15f)}
    "down"->{line(5f,9f,12f,16f);line(12f,16f,19f,9f)}
    "left"->{line(15f,5f,8f,12f);line(8f,12f,15f,19f)}
    "right"->{line(9f,5f,16f,12f);line(16f,12f,9f,19f)}
    "calendar"->{box(3f,5f,18f,16f);line(3f,10f,21f,10f);line(8f,3f,8f,7f);line(16f,3f,16f,7f)}
    "settings"->{drawCircle(color,8f,Offset(12f,12f),style=Stroke(1.7f));drawCircle(color,3f,Offset(12f,12f),style=Stroke(1.7f));listOf(0f,45f,90f,135f,180f,225f,270f,315f).forEach{a->rotate(a,pivot=Offset(12f,12f)){line(12f,1f,12f,4f)}}}
    "download","upload"->{val up=name=="upload";line(12f,if(up)17f else 3f,12f,if(up)3f else 17f);line(7f,if(up)8f else 12f,12f,if(up)3f else 17f);line(17f,if(up)8f else 12f,12f,if(up)3f else 17f);line(4f,18f,4f,21f);line(4f,21f,20f,21f);line(20f,18f,20f,21f)}
    "more"->listOf(5f,12f,19f).forEach{drawCircle(color,1.5f,Offset(it,12f))}
    "edit"->{line(5f,16f,16f,5f);line(16f,5f,20f,9f);line(20f,9f,9f,20f);line(9f,20f,4f,21f);line(4f,21f,5f,16f)}
    "餐饮"->{line(6f,3f,6f,21f);line(3f,3f,3f,9f);line(9f,3f,9f,9f);line(3f,9f,9f,9f);line(17f,3f,17f,21f);line(17f,3f,20f,11f);line(17f,11f,20f,11f)}
    "出行"->{box(4f,4f,16f,14f);line(4f,10f,20f,10f);line(7f,18f,7f,21f);line(17f,18f,17f,21f);drawCircle(color,1f,Offset(8f,14f));drawCircle(color,1f,Offset(16f,14f))}
    "加油"->{box(4f,3f,10f,18f);box(6f,5f,6f,6f,1f);line(3f,21f,15f,21f);line(14f,10f,19f,10f);line(19f,10f,19f,19f);line(19f,19f,21f,19f);line(21f,19f,21f,6f);line(18f,3f,21f,6f)}
    "停车"->{box(3f,3f,18f,18f);val path=Path().apply{moveTo(9f,18f);lineTo(9f,7f);lineTo(13f,7f);cubicTo(18f,7f,18f,13f,13f,13f);lineTo(9f,13f)};drawPath(path,color,style=Stroke(1.7f,cap=StrokeCap.Round))}
    "奖金"->{box(3f,8f,18f,5f,1f);box(5f,13f,14f,8f,1f);line(12f,8f,12f,21f);drawOval(color,Offset(6f,3f),Size(6f,5f),style=Stroke(1.7f));drawOval(color,Offset(12f,3f),Size(6f,5f),style=Stroke(1.7f))}
    "兼职"->{box(3f,7f,18f,14f);box(8f,3f,8f,4f,1f);line(3f,13f,21f,13f);line(10f,12f,14f,12f)}
    "退款"->{line(4f,8f,18f,8f);line(4f,8f,8f,4f);line(4f,8f,8f,12f);line(18f,8f,20f,12f);line(20f,12f,18f,18f);line(18f,18f,7f,18f)}
    "购物"->{box(4f,7f,16f,14f);drawArc(color,180f,180f,false,Offset(8f,2f),Size(8f,10f),style=Stroke(1.7f))}
    "居住"->{line(3f,11f,12f,3f);line(12f,3f,21f,11f);line(6f,9f,6f,21f);line(6f,21f,18f,21f);line(18f,21f,18f,9f)}
    "娱乐"->{box(3f,7f,18f,13f);line(7f,4f,10f,7f);line(17f,4f,14f,7f);line(10f,11f,15f,14f);line(15f,14f,10f,17f);line(10f,17f,10f,11f)}
    else->{box(3f,5f,18f,15f);line(3f,10f,21f,10f);line(16f,14f,19f,14f)}
   }
  }
 }
}
@Composable fun ActionIcon(icon:String,label:String,onClick:()->Unit){IconButton(onClick=onClick,modifier=Modifier.clip(CircleShape).semantics{contentDescription=label}){LedgerIcon(icon)}}
@Composable fun ScreenHeader(title:String,subtitle:String?=null,actions:@Composable RowScope.()->Unit={}){
 Row(Modifier.fillMaxWidth().padding(top=8.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.headlineLarge);subtitle?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=5.dp))}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp),content=actions)}
}
@Composable fun SectionHeading(title:String,action:String?=null,onAction:()->Unit={}){Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Text(title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));action?.let{TextButton(onClick=onAction){Text(it)}}}}
@Composable fun LedgerEmpty(title:String,description:String,action:String?=null,onAction:()->Unit={}){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.size(64.dp),contentAlignment=Alignment.Center){LedgerIcon("bills",Modifier.size(28.dp),MaterialTheme.colorScheme.primary)}};Text(title,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);action?.let{FilledTonalButton(onClick=onAction){Text(it)}}}}
@Composable fun Metric(label:String,value:String,modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.onSurface){Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)){Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,style=MaterialTheme.typography.titleMedium,color=color)}}
@Composable fun TransactionCell(t:Transaction,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
 Surface(shape=RoundedCornerShape(14.dp),color=if(t.type=="收入")MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh){Box(Modifier.size(46.dp),contentAlignment=Alignment.Center){LedgerIcon(t.category,color=if(t.type=="收入")MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}
 Column(Modifier.weight(1f)){Text(t.note.ifBlank{t.category},style=MaterialTheme.typography.titleSmall,maxLines=2);Text("${t.category} · ${ledgerDate(t.date,"HH:mm")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))}
 Text((if(t.type=="收入")"+" else "−")+ledgerMoney(t.amountCent),style=MaterialTheme.typography.titleMedium,color=if(t.type=="收入")MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
 }
}
@Composable fun SettingsRow(icon:String,title:String,subtitle:String,onClick:()->Unit,enabled:Boolean=true){Surface(onClick=onClick,enabled=enabled,color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(16.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){LedgerIcon(icon,color=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))};LedgerIcon("right",Modifier.size(16.dp))}}}
