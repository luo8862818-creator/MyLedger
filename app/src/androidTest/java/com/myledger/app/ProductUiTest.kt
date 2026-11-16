package com.myledger.app
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.myledger.app.data.database.AppDatabase
import com.myledger.app.data.entity.*
import com.myledger.app.data.repository.*
import com.myledger.app.ui.viewmodel.LedgerViewModel
import com.myledger.app.ui.theme.MyLedgerTheme
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProductUiTest {
 @get:Rule val compose=createComposeRule()
 private fun shot(name:String){compose.waitForIdle();Thread.sleep(300);val i=InstrumentationRegistry.getInstrumentation();var capture:android.graphics.Bitmap?=null;repeat(8){if(capture==null){Thread.sleep(250);val candidate=i.uiAutomation.takeScreenshot();if(candidate!=null){var drawn=0;for(x in 1..8)for(y in 1..8)if((candidate.getPixel(candidate.width*x/9,candidate.height*y/9) and 0xFFFFFF)!=0)drawn++;if(drawn>16)capture=candidate else candidate.recycle()}}};val bitmap=checkNotNull(capture){"System screenshot is unavailable or black"};File(i.targetContext.getExternalFilesDir(null),"ui-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 @Test fun allPrimaryFlowsAndScreenshots(){
  val ctx=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).build();val vm=LedgerViewModel(TransactionRepository(db.transactionDao()),ItemRepository(db.itemDao()));val store=ViewModelStore();store.put("preview",vm)
  val now=System.currentTimeMillis()
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant com.myledger.app android.permission.RECORD_AUDIO").close()
  runBlocking{
   listOf(Transaction(amountCent=865000,type="收入",category="工资",note="本月工资",date=now-4*86400000L),Transaction(amountCent=4200,type="支出",category="餐饮",note="咖啡与早餐",date=now-3600000),Transaction(amountCent=6800,type="支出",category="购物",note="周末生活采购",date=now-7200000),Transaction(amountCent=2800,type="支出",category="出行",note="下班打车",date=now-86400000L),Transaction(amountCent=350000,type="支出",category="居住",note="本月房租",date=now-2*86400000L)).forEach{db.transactionDao().insert(it)}
   db.itemDao().insert(Item(name="Sony WH-1000XM5",category="数码",brand="Sony",model="WH-1000XM5",purchasePriceCent=189900,purchaseDate=now-128*86400000L,note="通勤与专注时的陪伴"));db.itemDao().insert(Item(name="iPad Air",category="数码",brand="Apple",purchasePriceCent=479900,purchaseDate=now-365*86400000L))
  }
  try{
   compose.setContent{val view=androidx.compose.ui.platform.LocalView.current;androidx.compose.runtime.DisposableEffect(view){view.keepScreenOn=true;onDispose{view.keepScreenOn=false}};MyLedgerTheme{LedgerApp(vm)}}
   compose.waitUntil(15000){compose.onAllNodesWithText("咖啡与早餐").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithTag("nav-首页").assertIsSelected();shot("home")
   compose.onNodeWithText("账单",useUnmergedTree=true).performClick();compose.onNodeWithContentDescription("筛选账单").assertExists();compose.onNodeWithTag("nav-账单").assertIsSelected();compose.onNodeWithTag("nav-首页").assertIsNotSelected();shot("bills")
   compose.onNodeWithContentDescription("筛选账单").performClick();compose.onNodeWithText("筛选账单").assertExists();shot("filters");compose.onNodeWithText("取消").performClick()
   compose.onNodeWithText("咖啡与早餐").performClick();compose.onNodeWithText("账单详情").assertExists();shot("bill-detail");compose.onNodeWithContentDescription("关闭详情").performClick()
   compose.onNodeWithText("物品",useUnmergedTree=true).performClick();compose.onNodeWithText("Sony WH-1000XM5").assertExists();shot("items");compose.onNodeWithText("Sony WH-1000XM5").performClick();shot("item-detail");compose.onNodeWithContentDescription("关闭物品详情").performClick()
   compose.onNodeWithContentDescription("添加物品").performClick();compose.onNodeWithText("保存物品").assertIsDisplayed();shot("item-editor");compose.onNodeWithText("取消").performClick()
   compose.onNodeWithText("统计",useUnmergedTree=true).performClick();compose.onNodeWithText("收支统计").assertExists();shot("stats");compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("支出构成"));shot("stats-categories")
   compose.onNodeWithContentDescription("记一笔").performClick();compose.onNodeWithText("保存账单").assertIsDisplayed();shot("entry")
   compose.onNodeWithText("一句话记账").performClick();compose.onNodeWithText("描述这笔账").performTextInput("吃饭20，打车30");compose.waitUntil(10000){compose.onAllNodesWithText("保存 2 笔账单").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("保存 2 笔账单").performClick();compose.waitUntil(10000){compose.onAllNodesWithText("文字记账").fetchSemanticsNodes().isEmpty()};compose.runOnIdle{runBlocking{val batch=db.transactionDao().getAllTransactionsForExport().filter{it.note in listOf("吃饭20","打车30")};check(batch.size==2&&batch.sumOf{it.amountCent}==5000L&&batch.map{it.category}.toSet()==setOf("餐饮","出行"))}};compose.onNodeWithContentDescription("记一笔").performClick();compose.onNodeWithText("语音记账").performClick();compose.onNodeWithTag("voice-waveform").assertExists();shot("voice");compose.onNodeWithText("手动记账").performClick();compose.onNodeWithText("取消").performClick()
   compose.onNodeWithText("首页",useUnmergedTree=true).performClick();compose.onNodeWithContentDescription("数据与备份").performClick();compose.onNodeWithText("账单数据").assertExists();shot("backup");compose.onNodeWithContentDescription("关闭数据与备份").performClick()
  }finally{store.clear();db.close()}
 }
}
