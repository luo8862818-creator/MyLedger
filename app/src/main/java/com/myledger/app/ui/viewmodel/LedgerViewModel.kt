package com.myledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myledger.app.data.entity.Item
import com.myledger.app.data.entity.Transaction
import com.myledger.app.data.repository.ItemRepository
import com.myledger.app.data.repository.TransactionRepository
import com.myledger.app.domain.export.TransactionCsvFormatter
import com.myledger.app.domain.importer.ParsedTransactionCsv
import com.myledger.app.domain.importer.TransactionCsvImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LedgerViewModel(private val transactions: TransactionRepository, private val items: ItemRepository) : ViewModel() {
    val ui = combine(transactions.allTransactions, items.allItems) { tx, item -> LedgerData(tx, item) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerData())
    fun billTransactions(startMillis: Long, endExclusiveMillis: Long, type: String, query: String): Flow<List<Transaction>> =
        transactions.forBillRange(startMillis, endExclusiveMillis, type, query)
    fun saveTransaction(transaction: Transaction, onSaved: () -> Unit) = viewModelScope.launch {
        transactions.save(transaction)
        onSaved()
    }
    fun prepareTransactionsCsv(onReady: (String) -> Unit, onError: () -> Unit) = viewModelScope.launch {
        try {
            val csv = withContext(Dispatchers.IO) {
                TransactionCsvFormatter.format(transactions.allTransactionsForExport())
            }
            onReady(csv)
        } catch (exception: Exception) {
            if (exception is java.util.concurrent.CancellationException) throw exception
            onError()
        }
    }
    fun importTransactions(csv: String, onImported: (ParsedTransactionCsv, Int) -> Unit, onError: () -> Unit) = viewModelScope.launch {
        try {
            val parsed = withContext(Dispatchers.Default) { TransactionCsvImporter.parse(csv) }
            val imported = withContext(Dispatchers.IO) { transactions.importTransactions(parsed.transactions) }
            onImported(parsed, imported)
        } catch (exception: Exception) {
            if (exception is java.util.concurrent.CancellationException) throw exception
            onError()
        }
    }
    fun deleteTransaction(transaction: Transaction, onDeleted: () -> Unit = {}) = viewModelScope.launch {
        transactions.delete(transaction)
        onDeleted()
    }
    fun saveItem(item: Item) = viewModelScope.launch { items.save(item) }
    fun deleteItem(item: Item) = viewModelScope.launch { items.delete(item) }
}
data class LedgerData(val transactions: List<Transaction> = emptyList(), val items: List<Item> = emptyList())
