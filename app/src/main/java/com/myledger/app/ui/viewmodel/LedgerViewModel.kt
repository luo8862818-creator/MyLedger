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
    fun saveTransactions(batch: List<Transaction>, onError: () -> Unit = {}, onSaved: () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { transactions.saveAll(batch) }
            onSaved()
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            onError()
        }
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
    fun deleteTransaction(transaction: Transaction, onError: () -> Unit = {}, onDeleted: () -> Unit = {}) = viewModelScope.launch {
        try { transactions.delete(transaction); onDeleted() } catch(e: Exception) { if(e is java.util.concurrent.CancellationException) throw e; onError() }
    }
    suspend fun exportItems(): String = withContext(Dispatchers.IO) { com.myledger.app.domain.item.ItemCsv.format(items.allForExport()) }
    suspend fun previewItems(csv: String) = withContext(Dispatchers.IO) { com.myledger.app.domain.item.ItemCsv.parse(csv,items.allForExport()) }
    suspend fun importItems(preview: com.myledger.app.domain.item.ItemCsvPreview) = withContext(Dispatchers.IO) { items.importItems(preview.items) }
    fun saveItem(item: Item, onError: () -> Unit = {}, onSaved: () -> Unit = {}) = viewModelScope.launch { try { items.save(item); onSaved() } catch(e: Exception) { if(e is java.util.concurrent.CancellationException) throw e; onError() } }
    fun deleteItem(item: Item, onError: () -> Unit = {}, onDeleted: () -> Unit = {}) = viewModelScope.launch { try { items.delete(item); onDeleted() } catch(e: Exception) { if(e is java.util.concurrent.CancellationException) throw e; onError() } }
}
data class LedgerData(val transactions: List<Transaction> = emptyList(), val items: List<Item> = emptyList())
