package com.myledger.app


import android.app.Application
import androidx.room.Room
import com.myledger.app.data.database.AppDatabase

class MyLedgerApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_ledger.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
    }
}
