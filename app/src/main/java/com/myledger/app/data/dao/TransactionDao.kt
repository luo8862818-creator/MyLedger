package com.myledger.app.data.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.myledger.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImported(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @RoomTransaction
    suspend fun importTransactions(transactions: List<Transaction>): Int =
        transactions.count { insertImported(it) != -1L }

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAllTransactionsForExport(): List<Transaction>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date >= :startMillis AND date < :endExclusiveMillis
          AND (:type = '' OR type = :type)
          AND (
            :query = ''
            OR note COLLATE NOCASE LIKE '%' || :query || '%'
            OR category COLLATE NOCASE LIKE '%' || :query || '%'
          )
        ORDER BY date DESC, id DESC
        """
    )
    fun getForBillRange(
        startMillis: Long,
        endExclusiveMillis: Long,
        type: String,
        query: String
    ): Flow<List<Transaction>>

}
