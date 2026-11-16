package com.myledger.app.data.repository


import com.myledger.app.data.dao.TransactionDao
import com.myledger.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    val allTransactions: Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    suspend fun allTransactionsForExport(): List<Transaction> =
        transactionDao.getAllTransactionsForExport()

    suspend fun importTransactions(transactions: List<Transaction>): Int =
        transactionDao.importTransactions(transactions)

    fun forBillRange(startMillis: Long, endExclusiveMillis: Long, type: String, query: String): Flow<List<Transaction>> =
        transactionDao.getForBillRange(startMillis, endExclusiveMillis, type, query)

    suspend fun saveAll(transactions: List<Transaction>) = transactionDao.saveAll(transactions)

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}
