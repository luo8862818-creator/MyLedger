package com.myledger.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import com.myledger.app.ui.transaction.TransactionEntrySheet
import com.myledger.app.ui.theme.MyLedgerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class TransactionScrollBoundaryTest {
 @get:Rule val compose=createComposeRule()
 private var dismissed=false
 private fun render(){compose.setContent{val view=androidx.compose.ui.platform.LocalView.current;androidx.compose.runtime.DisposableEffect(view){view.keepScreenOn=true;onDispose{view.keepScreenOn=false}};MyLedgerTheme{TransactionEntrySheet(isSaving=false,onSave={},onDismiss={dismissed=true})}}}
 private fun bounds()=compose.onNodeWithTag("transaction-sheet").fetchSemanticsNode().boundsInRoot
 @Test fun topAndBottomSwipesNeverMoveOrDismissEditor(){
  render();val original=bounds()
  repeat(5){compose.onNodeWithTag("transaction-scroll").performTouchInput{swipeDown(durationMillis=350)}}
  compose.runOnIdle{assertFalse("At top, a downward content swipe must not dismiss the editor",dismissed)}
  assertEquals(original.top,bounds().top,1f)
  repeat(12){compose.onNodeWithTag("transaction-scroll").performTouchInput{swipeUp(durationMillis=150)}}
  compose.onNodeWithText("备注").assertIsDisplayed();compose.onNodeWithText("保存账单").assertIsDisplayed()
  val bottom=compose.onNodeWithTag("transaction-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
  assertEquals(bottom.maxValue(),bottom.value(),1f)
  repeat(8){compose.onNodeWithTag("transaction-scroll").performTouchInput{swipeUp(durationMillis=100)}}
  val after=compose.onNodeWithTag("transaction-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
  assertEquals(bottom.maxValue(),after.maxValue(),1f);assertEquals(after.maxValue(),after.value(),1f)
  assertEquals(original.top,bounds().top,1f);assertEquals(original.bottom,bounds().bottom,1f)
  compose.runOnIdle{assertFalse(dismissed)}
  repeat(12){compose.onNodeWithTag("transaction-scroll").performTouchInput{swipeDown(durationMillis=150)}}
  compose.onNodeWithContentDescription("金额").assertIsDisplayed();assertEquals(original.top,bounds().top,1f)
  compose.onNodeWithText("保存账单").assertIsDisplayed();compose.runOnIdle{assertFalse(dismissed)}
 }
 @Test fun keyboardScrollAndModeSwitchKeepEditorBounded(){
  render();compose.onNodeWithContentDescription("金额").performTextInput("25")
  compose.onNodeWithText("备注").performScrollTo().performTextInput("测试滚动时保留输入")
  val keyboardBounds=bounds()
  repeat(6){compose.onNodeWithTag("transaction-scroll").performTouchInput{swipeUp(durationMillis=150)}}
  compose.onNodeWithText("保存账单").assertIsDisplayed();assertEquals(keyboardBounds.top,bounds().top,1f)
  compose.onNodeWithText("一句话记账").performScrollTo().performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20")
  compose.onNodeWithText("保存账单").assertIsDisplayed();compose.onNodeWithText("手动记账").performClick()
  compose.onNodeWithContentDescription("金额").assertTextEquals("25")
  compose.onNodeWithText("备注").performScrollTo().assertTextContains("测试滚动时保留输入")
  compose.runOnIdle{assertFalse(dismissed)}
 }
}
