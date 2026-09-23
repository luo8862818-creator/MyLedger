package com.myledger.app.data.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myledger.app.data.dao.ItemDao
import com.myledger.app.data.dao.TransactionDao
import com.myledger.app.data.entity.Item
import com.myledger.app.data.entity.Transaction

@Database(
    entities = [Transaction::class, Item::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun itemDao(): ItemDao

    companion object {
        /** Preserves v1 records while converting legacy Double yuan values to integer cents. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions RENAME TO transactions_old")
                db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amountCent INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, date INTEGER NOT NULL, relatedItemId INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO transactions (id, amountCent, type, category, note, date, relatedItemId, createdAt, updatedAt) SELECT id, CAST(ROUND(amount * 100) AS INTEGER), type, category, note, date, NULL, date, date FROM transactions_old")
                db.execSQL("DROP TABLE transactions_old")
                db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, brand TEXT NOT NULL, model TEXT NOT NULL, purchasePriceCent INTEGER NOT NULL, purchaseDate INTEGER NOT NULL, warrantyEndDate INTEGER, note TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }
        /** Makes period, category and type filters scale without a full table scan. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type_date ON transactions(type, date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category_date ON transactions(category, date)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_category ON items(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_status ON items(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_purchaseDate ON items(purchaseDate)")
            }
        }
    }
}
