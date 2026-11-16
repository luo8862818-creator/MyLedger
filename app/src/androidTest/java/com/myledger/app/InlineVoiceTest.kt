package com.myledger.app
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.myledger.app.data.entity.Transaction
import com.myledger.app.ui.transaction.*
import com.myledger.app.ui.theme.MyLedgerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class InlineVoiceTest {
 @get:Rule val compose=createComposeRule()
 private class Speech:LedgerSpeechEngine {
  override val available=true
  var final:((String)->Unit)?=null;var partial:((String)->Unit)?=null
  override fun start(onReady:()->Unit,onLevel:(Float)->Unit,onPartial:(String)->Unit,onFinal:(String)->Unit,onError:(Int)->Unit,onEnd:()->Unit){partial=onPartial;final=onFinal;onReady();onLevel(8f)}
  override fun stop(){};override fun cancel(){};override fun destroy(){}
 }
 private fun render(speech:Speech=Speech(),initial:Transaction?=null,save:(List<Transaction>)->Unit){
  val i=InstrumentationRegistry.getInstrumentation();i.uiAutomation.executeShellCommand("pm grant com.myledger.app android.permission.RECORD_AUDIO").close()
  compose.setContent{val view=androidx.compose.ui.platform.LocalView.current;androidx.compose.runtime.DisposableEffect(view){view.keepScreenOn=true;onDispose{view.keepScreenOn=false}};CompositionLocalProvider(LocalLedgerSpeechFactory provides {speech}){MyLedgerTheme{TransactionEntrySheet(isSaving=false,initial=initial,onDismiss={},onSave=save)}}}
 }
 private fun shot(name:String){
  compose.waitForIdle();val i=InstrumentationRegistry.getInstrumentation();var image:Bitmap?=null
  repeat(8){if(image==null){val candidate=i.uiAutomation.takeScreenshot();if(candidate!=null){val small=Bitmap.createScaledBitmap(candidate,64,128,true);val colors=mutableSetOf<Int>();for(x in 0 until small.width)for(y in 0 until small.height)colors+=small.getPixel(x,y);small.recycle();if(colors.size>20)image=candidate else candidate.recycle()};if(image==null)Thread.sleep(250)}}
  val bitmap=checkNotNull(image){"实际界面截图为黑帧"};File(i.targetContext.getExternalFilesDir(null),"smart-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
 }
 @Test fun descriptionSavesWithoutManualAmount(){
  val saved=mutableListOf<Transaction>();render{saved+=it}
  compose.onNodeWithText("一句话记账").performClick()
  compose.onAllNodes(isDialog()).assertCountEquals(1)
  compose.onNodeWithContentDescription("金额").assertDoesNotExist()
  compose.onNodeWithText("选择分类").assertDoesNotExist()
  compose.onNodeWithText("填入账单").assertDoesNotExist()
  compose.onNodeWithText("描述这笔账").performTextInput("吃饭20")
  compose.waitUntil(10000){compose.onAllNodesWithText("¥ 20.00").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("¥ 20.00").assertIsDisplayed();shot("description")
  compose.onNodeWithText("保存账单").assertIsDisplayed().performClick()
  compose.waitUntil(10000){saved.size==1}
  compose.runOnIdle{assertEquals(2000L,saved.single().amountCent);assertEquals("餐饮",saved.single().category);assertEquals("支出",saved.single().type);assertEquals("吃饭20",saved.single().note)}
 }
 @Test fun missingAmountStaysInDescriptionAndCanBeCorrected(){
  val saved=mutableListOf<Transaction>();render{saved+=it}
  compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭")
  compose.onNodeWithText("保存账单").performClick();compose.waitUntil(10000){compose.onAllNodesWithTag("smart-entry-error").fetchSemanticsNodes().isNotEmpty()}
  compose.runOnIdle{assertTrue(saved.isEmpty())};compose.onNodeWithContentDescription("金额").assertDoesNotExist()
  compose.onNodeWithText("描述这笔账").performTextReplacement("吃饭二十元");compose.onNodeWithText("保存账单").performClick()
  compose.waitUntil(10000){saved.size==1};compose.runOnIdle{assertEquals(2000L,saved.single().amountCent)}
 }
 @Test fun voiceFinalSavesOnceAndPartialNeverSaves(){
  val saved=mutableListOf<Transaction>();val speech=Speech();render(speech){saved+=it}
  compose.onNodeWithText("语音记账").performClick();compose.waitUntil(10000){speech.final!=null}
  compose.onNodeWithTag("voice-waveform").assertExists();compose.onAllNodes(isDialog()).assertCountEquals(1)
  compose.runOnIdle{speech.partial?.invoke("午饭二十")};compose.runOnIdle{assertTrue(saved.isEmpty())};shot("voice")
  compose.runOnIdle{speech.final?.invoke("午饭二十五元");speech.final?.invoke("午饭二十五元")}
  compose.waitUntil(10000){saved.size==1};compose.runOnIdle{assertEquals(2500L,saved.single().amountCent);assertEquals("餐饮",saved.single().category)}
 }
 @Test fun unsuccessfulSaveCanBeRetried(){
  var attempts=0
  render{attempts++}
  compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20")
  compose.onNodeWithText("保存账单").performClick();compose.waitUntil(10000){attempts==1}
  compose.onNodeWithText("保存账单").assertIsEnabled().performClick();compose.waitUntil(10000){attempts==2}
 }
 @Test fun lateVoiceResultAfterSwitchCannotSave(){
  val saved=mutableListOf<Transaction>();val speech=Speech();render(speech){saved+=it}
  compose.onNodeWithText("语音记账").performClick();compose.waitUntil(10000){speech.final!=null}
  compose.onNodeWithText("改用文字").performClick();compose.runOnIdle{speech.final?.invoke("吃饭20")}
  compose.waitForIdle();compose.runOnIdle{assertTrue(saved.isEmpty())};compose.onNodeWithText("描述这笔账").assertExists();compose.onNodeWithTag("voice-waveform").assertDoesNotExist()
 }
 @Test fun descriptionSplitsAndSubmitsOneBatch(){
  var calls=0;val saved=mutableListOf<Transaction>();render{calls++;saved+=it}
  compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20，打车30")
  compose.waitUntil(10000){compose.onAllNodesWithText("已识别 2 笔").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("保存 2 笔账单").assertIsDisplayed();shot("batch-preview");compose.onNodeWithText("保存 2 笔账单").performClick()
  compose.waitUntil(10000){saved.size==2};compose.runOnIdle{assertEquals(1,calls);assertEquals(listOf(2000L,3000L),saved.map{it.amountCent});assertEquals(listOf("餐饮","出行"),saved.map{it.category})}
 }
 @Test fun voiceBatchSavesOnceWithNoPartialWrites(){
  var calls=0;val saved=mutableListOf<Transaction>();val speech=Speech();render(speech){calls++;saved+=it}
  compose.onNodeWithText("语音记账").performClick();compose.waitUntil(10000){speech.final!=null}
  compose.runOnIdle{speech.partial?.invoke("吃饭20，打车")};compose.runOnIdle{assertTrue(saved.isEmpty());speech.final?.invoke("吃饭20，打车30");speech.final?.invoke("吃饭20，打车30")}
  compose.waitUntil(10000){saved.size==2};compose.runOnIdle{assertEquals(1,calls);assertEquals(5000L,saved.sumOf{it.amountCent})}
 }
 @Test fun incompleteBatchNeverSavesOnlyTheValidPart(){
  val saved=mutableListOf<Transaction>();render{saved+=it}
  compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20，打车")
  compose.waitUntil(10000){compose.onAllNodesWithText("保存 2 笔账单").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("保存 2 笔账单").performClick();compose.waitUntil(10000){compose.onAllNodesWithTag("smart-entry-error").fetchSemanticsNodes().isNotEmpty()}
  compose.runOnIdle{assertTrue(saved.isEmpty())}
 }

 @Test fun editingOneBillCannotOverwriteItWithTwoBills(){
  val saved=mutableListOf<Transaction>();render(initial=Transaction(id=7,amountCent=1000,type="支出",category="餐饮",date=1000)){saved+=it}
  compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20，打车30")
  compose.waitUntil(10000){compose.onAllNodesWithText("保存 2 笔账单").fetchSemanticsNodes().isNotEmpty()}
  compose.waitUntil(10000){compose.onAllNodesWithText("保存 2 笔账单").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("保存 2 笔账单").performClick();compose.waitUntil(10000){compose.onAllNodesWithTag("smart-entry-error").fetchSemanticsNodes().isNotEmpty()};compose.runOnIdle{assertTrue(saved.isEmpty())}
 }

}
