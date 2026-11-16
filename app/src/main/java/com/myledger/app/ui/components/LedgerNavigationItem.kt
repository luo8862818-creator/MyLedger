package com.myledger.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable fun LedgerNavigationItem(icon:String,label:String,isSelected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){
 Column(modifier.clickable(role=Role.Tab,onClick=onClick).testTag("nav-$label").padding(vertical=6.dp).semantics{selected=isSelected},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
  Surface(modifier=Modifier.size(width=52.dp,height=32.dp),shape=CircleShape,color=if(isSelected)MaterialTheme.colorScheme.primary else Color.Transparent){
   Box(contentAlignment=Alignment.Center){LedgerIcon(icon,color=if(isSelected)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)}
  }
  Text(label,style=MaterialTheme.typography.labelSmall,fontWeight=if(isSelected)FontWeight.Bold else FontWeight.Normal,color=if(isSelected)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
