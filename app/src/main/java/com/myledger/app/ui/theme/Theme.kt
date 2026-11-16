package com.myledger.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
private val Light=lightColorScheme(
 primary=Color(0xFF237B70),onPrimary=Color.White,primaryContainer=Color(0xFFD8EBDE),onPrimaryContainer=Color(0xFF12695F),
 secondary=Color(0xFF63766D),secondaryContainer=Color(0xFFF3E3C6),onSecondaryContainer=Color(0xFF66502C),
 background=Color(0xFFF2EEE6),onBackground=Color(0xFF202630),surface=Color(0xFFFFFBF4),onSurface=Color(0xFF202630),
 surfaceVariant=Color(0xFFECE6DA),onSurfaceVariant=Color(0xFF69766C),surfaceContainer=Color(0xFFECE6DA),surfaceContainerLow=Color.White,surfaceContainerHigh=Color(0xFFE8E7DB),surfaceContainerHighest=Color(0xFFE6EAE3),
 outline=Color(0xFFADB6AE),outlineVariant=Color(0xFFE3DDCF),error=Color(0xFFD47A5F),errorContainer=Color(0xFFFFEDE7))
private val Dark=darkColorScheme(
 primary=Color(0xFF8CD6BA),onPrimary=Color(0xFF12392D),primaryContainer=Color(0xFF224B3D),onPrimaryContainer=Color(0xFFB9EBD5),
 secondary=Color(0xFFB0C7BA),secondaryContainer=Color(0xFF35493E),onSecondaryContainer=Color(0xFFE4EDE6),background=Color(0xFF121A16),onBackground=Color(0xFFE4EDE6),surface=Color(0xFF1C2620),onSurface=Color(0xFFE4EDE6),
 surfaceVariant=Color(0xFF303C33),onSurfaceVariant=Color(0xFFADB9AF),surfaceContainer=Color(0xFF1C2620),surfaceContainerLow=Color(0xFF1C2620),surfaceContainerHigh=Color(0xFF27332B),surfaceContainerHighest=Color(0xFF303C33),outline=Color(0xFF728277),outlineVariant=Color(0xFF354239),error=Color(0xFFFFB19B),errorContainer=Color(0xFF59372D))
@Composable fun MyLedgerTheme(darkTheme:Boolean=isSystemInDarkTheme(),dynamicColor:Boolean=false,content:@Composable ()->Unit){
 val view=LocalView.current
 if(!view.isInEditMode){
  val activity=generateSequence(view.context){(it as? ContextWrapper)?.baseContext}.filterIsInstance<Activity>().firstOrNull()
  SideEffect{activity?.window?.let{window->WindowCompat.getInsetsController(window,view).apply{isAppearanceLightStatusBars=!darkTheme;isAppearanceLightNavigationBars=!darkTheme}}}
 }
 MaterialTheme(colorScheme=if(darkTheme)Dark else Light,typography=Typography,shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(14.dp),large=RoundedCornerShape(18.dp),extraLarge=RoundedCornerShape(24.dp)),content=content)}
