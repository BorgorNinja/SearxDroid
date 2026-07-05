package dev.searxdroid.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.searxdroid.app.data.model.SearchCategory
import dev.searxdroid.app.data.model.SearxResult
import dev.searxdroid.app.data.repository.SearchRepository
import dev.searxdroid.app.data.repository.SearchState
import dev.searxdroid.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val searchRepo   = SearchRepository.getInstance()
    private val settingsRepo = SettingsRepository.getInstance(app)

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _query    = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow(SearchCategory.GENERAL)
    val category: StateFlow<SearchCategory> = _category.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    fun onQueryChange(q: String) { _query.value = q }

    fun onCategoryChange(cat: SearchCategory) {
        _category.value = cat
        if (_query.value.isNotBlank()) search(_query.value, cat)
    }

    fun search(q: String = _query.value, cat: SearchCategory = _category.value, page: Int = 1) {
        if (q.isBlank()) return
        _query.value       = q
        _category.value    = cat
        _currentPage.value = page
        _searchState.value = SearchState.Loading

        viewModelScope.launch {
            val instanceUrl = settingsRepo.instanceUrl.first()
            if (instanceUrl.isBlank()) {
                _searchState.value = SearchState.Error(
                    message   = "No instance configured. " +
                                "Add a SearXNG instance URL in \u2699 Settings to get started.",
                    retryable = false,
                )
                return@launch
            }
            val safeSearch = settingsRepo.safeSearch.first()
            val language   = settingsRepo.language.first()

            _searchState.value = searchRepo.search(
                query             = q,
                instanceUrl       = instanceUrl,
                categories        = cat.id,
                language          = language,
                safeSearch        = safeSearch,
                page              = page,
                fallbackInstances = emptyList(),
            )
        }
    }

    /**
     * Load the next page of results and **merge** them with the currently
     * displayed results rather than replacing them.
     *
     * Deduplication by URL is applied on merge so that results found by
     * multiple engines (which may also appear on subsequent pages) don't
     * create duplicate list items — and more importantly don't trigger a
     * Compose duplicate-key crash.
     */
    fun loadNextPage() {
        val currentState = _searchState.value as? SearchState.Success ?: return
        val previousResults: List<SearxResult> = currentState.response.results
        val nextPage = _currentPage.value + 1

        _currentPage.value = nextPage
        _searchState.value = SearchState.Loading

        viewModelScope.launch {
            val instanceUrl = settingsRepo.instanceUrl.first()
            if (instanceUrl.isBlank()) return@launch

            val safeSearch = settingsRepo.safeSearch.first()
            val language   = settingsRepo.language.first()

            val result = searchRepo.search(
                query       = _query.value,
                instanceUrl = instanceUrl,
                categories  = _category.value.id,
                language    = language,
                safeSearch  = safeSearch,
                page        = nextPage,
            )

            _searchState.value = when (result) {
                is SearchState.Success -> {
                    // Merge: previous results first, then new results.
                    // distinctBy(url) removes any cross-page duplicates.
                    val merged = (previousResults + result.response.results)
                        .distinctBy { it.url }
                    SearchState.Success(
                        response    = result.response.copy(results = merged),
                        query       = result.query,
                        instanceUrl = result.instanceUrl,
                    )
                }
                else -> result   // Error or edge case — just show it
            }
        }
    }

    fun retry() = search()

    fun clear() {
        _query.value       = ""
        _searchState.value = SearchState.Idle
        _currentPage.value = 1
    }
}
