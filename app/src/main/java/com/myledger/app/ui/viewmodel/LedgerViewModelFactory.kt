package com.myledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.myledger.app.data.repository.ItemRepository
import com.myledger.app.data.repository.TransactionRepository

class LedgerViewModelFactory(private val tx: TransactionRepository, private val item: ItemRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LedgerViewModel(tx, item) as T
}
