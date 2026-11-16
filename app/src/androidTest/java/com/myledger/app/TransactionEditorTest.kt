package com.myledger.app
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.myledger.app.data.entity.Transaction
import com.myledger.app.ui.transaction.TransactionEntrySheet
import com.myledger.app.ui.theme.MyLedgerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class TransactionEditorTest {
 @get:Rule val compose=createComposeRule()
 @Test fun populatedEditorCancelDoesNotSave(){var saved=false;var dismissed=false
 compose.setContent{MyLedgerTheme{TransactionEntrySheet(isSaving=false,initial=Transaction(id=7,amountCent=1500,type="支出",category="餐饮",date=System.currentTimeMillis()),onSave={saved=true},onDismiss={dismissed=true})}}
 compose.onNodeWithText("编辑账单").assertIsDisplayed();compose.onNodeWithText("15.00").assertExists();compose.onNodeWithText("取消").performClick();compose.runOnIdle{assertTrue(dismissed);assertFalse(saved)}
 }
 @Test fun tenConsecutiveOpenAndClose(){
 val open=androidx.compose.runtime.mutableStateOf(false)
 compose.setContent{MyLedgerTheme{androidx.compose.material3.Button(onClick={open.value=true}){androidx.compose.material3.Text("打开测试")};if(open.value)TransactionEntrySheet(isSaving=false,onSave={},onDismiss={open.value=false})}}
 val times=mutableListOf<Long>()
 repeat(10){val start=android.os.SystemClock.elapsedRealtime();compose.onNodeWithText("打开测试").performClick();compose.onNodeWithText("记一笔").assertIsDisplayed();times+=android.os.SystemClock.elapsedRealtime()-start;compose.onNodeWithText("取消").performClick();compose.onNodeWithText("记一笔").assertDoesNotExist()}
 android.util.Log.i("LedgerPerformance","Debug instrumentation click-to-visible (including test synchronization), ms: $times")
 }
 @Test fun keyboardAndLargeTextKeepSaveVisible(){
 compose.setContent{val d=androidx.compose.ui.platform.LocalDensity.current;androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density,1.3f)){MyLedgerTheme(darkTheme=true){TransactionEntrySheet(isSaving=false,onSave={},onDismiss={})}}}
 compose.onNodeWithContentDescription("金额").performClick().performTextInput("25.80")
 compose.onNodeWithText("保存账单").assertIsDisplayed()
 val i=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();Thread.sleep(350);var capture:android.graphics.Bitmap?=null;repeat(5){if(capture==null){Thread.sleep(200);capture=i.uiAutomation.takeScreenshot()}};val bitmap=checkNotNull(capture){"System screenshot unavailable"};java.io.File(i.targetContext.getExternalFilesDir(null),"ui-keyboard-dark.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
 }
}
