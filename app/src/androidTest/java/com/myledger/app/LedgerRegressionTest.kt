package com.myledger.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.myledger.app.data.database.AppDatabase
import com.myledger.app.data.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LedgerRegressionTest {
 @Test fun editPreservesIdentityAndRefreshesRange()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
  try{val dao=db.transactionDao();val original=Transaction(amountCent=1500,type="支出",category="餐饮",date=1000,createdAt=500,updatedAt=500);val id=dao.insert(original);dao.update(original.copy(id=id,type="收入",category="工资",amountCent=800000,date=2000,updatedAt=3000));val saved=dao.getAllTransactionsForExport().single();assertEquals(id,saved.id);assertEquals(500L,saved.createdAt);assertEquals(3000L,saved.updatedAt);assertEquals(2000L,saved.date);assertEquals("收入",saved.type);assertEquals(800000L,saved.amountCent)}finally{db.close()}
 }
 @Test fun duplicateImportAndAtomicRollback()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
  try{val dao=db.itemDao();val i=Item(name="手机",category="数码",purchasePriceCent=12345,purchaseDate=100000);assertEquals(1,dao.importItems(listOf(i)));assertEquals(0,dao.importItems(listOf(i.copy(id=999))));
   db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_test BEFORE INSERT ON items WHEN NEW.name = 'FAIL' BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
   assertTrue(runCatching{dao.importItems(listOf(i.copy(name="NEW"),i.copy(name="FAIL")))}.isFailure);assertEquals(1,dao.allForExport().size)
  }finally{db.close()}
 }
 @Test fun transactionImportHandlesForeignIdsAndNoIds()=runBlocking {
 val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
 try{val dao=db.transactionDao();val original=Transaction(id=23,amountCent=100,type="支出",category="餐饮",date=1000,note="existing");dao.insert(original);val foreign=original.copy(amountCent=200,note="backup");assertEquals(1,dao.importTransactions(listOf(foreign)));assertEquals(original,dao.getAllTransactionsForExport().first{it.id==23L});assertEquals(2,dao.getAllTransactionsForExport().size);assertEquals(0,dao.importTransactions(listOf(foreign)));assertEquals(0,dao.importTransactions(listOf(foreign.copy(id=0))));assertEquals(2,dao.getAllTransactionsForExport().size)}finally{db.close()}
 }
 @Test fun identicalPurchasesWithDifferentIdsArePreserved()=runBlocking {
 val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
 try{val dao=db.transactionDao();val t=Transaction(id=51,amountCent=1500,type="支出",category="餐饮",date=1000);val rows=listOf(t,t.copy(id=52));assertEquals(2,dao.importTransactions(rows));assertEquals(0,dao.importTransactions(rows));assertEquals(2,dao.getAllTransactionsForExport().size)}finally{db.close()}
 }
 @Test fun submittedBatchIsAtomicAndKeepsSeparateRows()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
  try{val dao=db.transactionDao();val first=Transaction(amountCent=2000,type="支出",category="餐饮",date=1000,note="吃饭20");val second=first.copy(amountCent=3000,category="出行",note="打车30")
   dao.saveAll(listOf(first,second));val rows=dao.getAllTransactionsForExport();assertEquals(2,rows.size);assertEquals(5000L,rows.sumOf{it.amountCent});assertEquals(2,rows.map{it.id}.distinct().size)
   db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_batch BEFORE INSERT ON transactions WHEN NEW.note = 'FAIL' BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
   assertTrue(runCatching{dao.saveAll(listOf(first.copy(note="NEW"),second.copy(note="FAIL")))}.isFailure)
   assertEquals(rows,dao.getAllTransactionsForExport())
  }finally{db.close()}
 }

}
