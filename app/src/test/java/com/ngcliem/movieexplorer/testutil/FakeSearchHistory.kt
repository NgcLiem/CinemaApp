package com.ngcliem.movieexplorer.testutil

import com.ngcliem.movieexplorer.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSearchHistory : SearchHistoryRepository {
    private val items = MutableStateFlow<List<String>>(emptyList())
    override val history: StateFlow<List<String>> = items

    override fun add(query: String) {
        items.value = listOf(query) + (items.value - query)
    }

    override fun remove(query: String) {
        items.value = items.value - query
    }

    override fun clear() {
        items.value = emptyList()
    }
}
