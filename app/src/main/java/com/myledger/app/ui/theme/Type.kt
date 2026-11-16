package com.myledger.app.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
private fun style(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight,letterSpacing=0.sp)
val Typography=Typography(displaySmall=style(36,44,FontWeight.SemiBold),headlineLarge=style(26,34,FontWeight.Bold),headlineMedium=style(24,32,FontWeight.Bold),headlineSmall=style(24,32,FontWeight.SemiBold),titleLarge=style(18,26,FontWeight.SemiBold),titleMedium=style(16,24,FontWeight.SemiBold),titleSmall=style(15,22,FontWeight.Medium),bodyLarge=style(16,25),bodyMedium=style(14,22),bodySmall=style(12,19),labelLarge=style(14,20,FontWeight.SemiBold),labelMedium=style(12,18,FontWeight.Medium),labelSmall=style(11,16))
