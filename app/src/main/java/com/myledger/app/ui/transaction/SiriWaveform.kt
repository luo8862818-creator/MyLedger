package com.myledger.app.ui.transaction

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Decorative motion is separate from measured voice energy; never fabricates a transcript. */
@Composable internal fun SiriWaveform(levels:List<Float>,active:Boolean,processing:Boolean,modifier:Modifier=Modifier){
 val energy=remember(levels){levels.average().toFloat().coerceIn(0f,1f)}
 val brushes=remember{listOf(
  Brush.horizontalGradient(listOf(Color(0xFF55E4D4),Color(0xFF63A9FF),Color(0xFFBE8DFF))),
  Brush.horizontalGradient(listOf(Color(0xFF68B6FF),Color(0xFFCC93FF),Color(0xFFFF91BE))),
  Brush.horizontalGradient(listOf(Color(0xFF85EDD4),Color(0xFF89BFFF),Color(0xFFF4B0E3))) )}
 val phaseState:State<Float> = if(active||processing){
  rememberInfiniteTransition(label="voice-wave").animateFloat(0f,(PI*2).toFloat(),infiniteRepeatable(tween(1800,easing=LinearEasing),RepeatMode.Restart),label="wave-phase")
 }else remember{mutableFloatStateOf(0f)}

 Canvas(modifier.fillMaxWidth().height(76.dp).testTag("voice-waveform")){
  val phase=phaseState.value
  val amplitude=size.height*(if(active).07f+.34f*energy else if(processing).12f else .045f)
  for(index in 0..2){
   val path=Path()
   for(step in 0..80){val t=step/80f;val envelope=sin(PI*t).toFloat().pow(2);val y=size.height/2+sin(t*PI*4+phase+index*.8).toFloat()*amplitude*envelope*(1f-index*.18f)
    if(step==0)path.moveTo(0f,y)else path.lineTo(t*size.width,y)
   }
   drawPath(path,brushes[index],alpha=.12f,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
   drawPath(path,brushes[index],alpha=.9f,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
  }
 }
}
