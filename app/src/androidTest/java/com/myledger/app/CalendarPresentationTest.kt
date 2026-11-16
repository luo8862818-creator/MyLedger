package com.myledger.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.myledger.app.data.entity.Transaction
import com.myledger.app.domain.bill.BillPeriod
import com.myledger.app.domain.statistics.LedgerCalendar
import com.myledger.app.ui.screens.BillFiltersDialog
import com.myledger.app.ui.statistics.InsightStatistics
import com.myledger.app.ui.theme.MyLedgerTheme
import com.myledger.app.ui.viewmodel.LedgerData
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

class CalendarPresentationTest {
    @get:Rule val compose=createComposeRule()
    private fun shot(name:String){
        compose.waitForIdle();Thread.sleep(400)
        val i=InstrumentationRegistry.getInstrumentation()
        var image:Bitmap?=null
        repeat(8){
            if(image==null){
                val candidate=i.uiAutomation.takeScreenshot()
                if(candidate!=null){
                    val small=Bitmap.createScaledBitmap(candidate,64,128,true)
                    val colors=mutableSetOf<Int>();var lit=0
                    for(x in 0 until small.width)for(y in 0 until small.height){val pixel=small.getPixel(x,y);colors+=pixel;if(android.graphics.Color.red(pixel)+android.graphics.Color.green(pixel)+android.graphics.Color.blue(pixel)>30)lit++}
                    small.recycle()
                    if(colors.size>20&&lit>1024)image=candidate else candidate.recycle()
                }
                if(image==null)Thread.sleep(200)
            }
        }
        val bitmap=checkNotNull(image){"Screenshot is blank"}
        File(i.targetContext.getExternalFilesDir(null),"calendar-fix-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun expandedCalendarHasOnlyOneMonthHeader(){
        val date=LedgerCalendar.at(2026,11,16)
        compose.setContent{val view=androidx.compose.ui.platform.LocalView.current;androidx.compose.runtime.DisposableEffect(view){view.keepScreenOn=true;onDispose{view.keepScreenOn=false}};MyLedgerTheme{InsightStatistics(LedgerData(transactions=listOf(Transaction(id=1,amountCent=1500,type="支出",category="餐饮",date=date))))}}
        compose.waitUntil(15000){compose.onAllNodesWithTag("category-chart").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("expanded-calendar").assertDoesNotExist()
        compose.onNodeWithContentDescription("展开统计日历").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithTag("day-2026-11-16").fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText("2026年11月").assertCountEquals(1)
        compose.onNodeWithContentDescription("统计上一周期").assertExists()
        compose.onNodeWithContentDescription("统计下一周期").assertExists()
        compose.onNodeWithContentDescription("日历上个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("日历下个月").assertDoesNotExist()
        shot("single-header")
        compose.onNodeWithText("收起日历").assertDoesNotExist();compose.onNodeWithContentDescription("收起统计日历").performClick()
        compose.onNodeWithTag("expanded-calendar").assertDoesNotExist()
        compose.onNodeWithContentDescription("统计上一周期").assertIsDisplayed()
        compose.onAllNodesWithText("2026年11月").assertCountEquals(1)
    }
    @Test fun billFilterCalendarIsChineseWithoutChangingSystemLocale(){
        val originalLocale=Locale.getDefault()
        val original=LedgerCalendar.at(2026,11,16)
        var appliedDate:Long?=null
        compose.setContent{val view=androidx.compose.ui.platform.LocalView.current;androidx.compose.runtime.DisposableEffect(view){view.keepScreenOn=true;onDispose{view.keepScreenOn=false}};MyLedgerTheme{BillFiltersDialog(BillPeriod.MONTH,"","","",original,onApply={_,_,_,_,d->appliedDate=d},onDismiss={})}}
        compose.onNodeWithText("当前周期").performClick()
        compose.onNodeWithText("选择日期").assertExists()
        compose.onAllNodesWithText("2026年11月").assertCountEquals(2)
        compose.onNodeWithText("November 2026").assertDoesNotExist()
        compose.onNodeWithText("Select date").assertDoesNotExist()
        shot("chinese-picker")
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("确认筛选").performClick()
        compose.runOnIdle{assertEquals(originalLocale,Locale.getDefault());assertNotNull(appliedDate);val c=Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply{timeInMillis=appliedDate!!};assertEquals(16,c.get(Calendar.DAY_OF_MONTH))}
    }
}
