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

    /** One commit for an entire user submission; a failed row rolls back every row. */
    @RoomTransaction
    suspend fun saveAll(transactions:List<Transaction>) {
        require(transactions.isNotEmpty())
        transactions.forEach { transaction ->
            require(transaction.amountCent>0 && transaction.type in listOf("收入","支出") && transaction.category.isNotBlank())
            if(transaction.id==0L) insert(transaction) else update(transaction)
        }
    }

    @RoomTransaction
    suspend fun importTransactions(transactions: List<Transaction>): Int {
        val existing = getAllTransactionsForExport()
        fun key(t: Transaction) = listOf(t.date.toString(), t.type, t.category, t.amountCent.toString(), t.note)
        val existingCounts = existing.groupingBy(::key).eachCount()
        val incomingCounts = mutableMapOf<List<String>, Int>()
        val sourceRows = mutableSetOf<Pair<Long, List<String>>>()
        val ids = existing.map { it.id }.toMutableSet()
        var count = 0
        for (transaction in transactions) {
            val fingerprint = key(transaction)
            if (transaction.id > 0 && !sourceRows.add(transaction.id to fingerprint)) continue
            val ordinal = (incomingCounts[fingerprint] ?: 0) + 1
            incomingCounts[fingerprint] = ordinal
            // Match multiplicities: two legitimate identical purchases with different IDs remain two.
            if (ordinal <= (existingCounts[fingerprint] ?: 0)) continue
            // A backup from a different installation can reuse an existing primary key.
            val candidate = if (transaction.id in ids) transaction.copy(id = 0) else transaction
            val id = insertImported(candidate)
            check(id != -1L) { "无法导入账单，事务已回滚" }
            ids.add(id)
            count++
        }
        return count
    }

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
