package com.myledger.app.domain.item
import com.myledger.app.data.entity.Item
import org.junit.Assert.*
import org.junit.Test
class ItemCsvTest {
 private val item=Item(name="中文,\"手机\"\n新款",category="数码",brand="品牌",purchasePriceCent=12345,purchaseDate=1700000000000,status="使用中")
 @Test fun roundTrip(){val p=ItemCsv.parse(ItemCsv.format(listOf(item)),emptyList());assertEquals(0,p.errors.size);assertEquals(item.name,p.items.single().name);assertEquals(12345L,p.items.single().purchasePriceCent);assertEquals(item.createdAt,p.items.single().createdAt)}
 @Test fun duplicateAcrossDevices(){val p=ItemCsv.parse(ItemCsv.format(listOf(item.copy(id=999),item)),listOf(item.copy(id=12)));assertEquals(2,p.duplicates);assertTrue(p.items.isEmpty())}
 @Test fun invalidRecords(){val csv=ItemCsv.format(listOf(item.copy(status="错误"),item.copy(purchasePriceCent=0)));val p=ItemCsv.parse(csv,emptyList());assertEquals(2,p.errors.size);assertTrue(p.items.isEmpty())}
 @Test fun malformed(){listOf("", "a,b", "\"未闭合").forEach{assertTrue(runCatching{ItemCsv.parse(it,emptyList())}.isFailure)}}
}
