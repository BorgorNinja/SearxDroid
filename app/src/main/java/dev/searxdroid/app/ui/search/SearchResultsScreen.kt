package dev.searxdroid.app.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.searxdroid.app.data.model.SearchCategory
import dev.searxdroid.app.data.repository.SearchState
import dev.searxdroid.app.ui.components.CategoryChip
import dev.searxdroid.app.ui.components.ImageThumbnailCard
import dev.searxdroid.app.ui.components.ResultCard
import dev.searxdroid.app.ui.components.SearxSearchBar

@Composable
fun SearchResultsScreen(
    viewModel: SearchViewModel,
    initialQuery: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val searchState by viewModel.searchState.collectAsState()
    val query       by viewModel.query.collectAsState()
    val category    by viewModel.category.collectAsState()
    val listState   = rememberLazyListState()
    val gridState   = rememberLazyGridState()

    LaunchedEffect(initialQuery) {
        if (query.isBlank() || query != initialQuery) viewModel.search(initialQuery)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                        }
                    },
                    title = {
                        SearxSearchBar(
                            query         = query,
                            onQueryChange = viewModel::onQueryChange,
                            onSearch      = { viewModel.search(query) },
                            compact       = true,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background),
                )
                LazyRow(
                    modifier              = Modifier.fillMaxWidth(),
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(SearchCategory.entries) { cat ->
                        CategoryChip(
                            label    = cat.label,
                            selected = cat == category,
                            onClick  = { viewModel.onCategoryChange(cat) },
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        when (val state = searchState) {
            is SearchState.Idle    -> Unit
            is SearchState.Loading -> LoadingIndicator(Modifier.padding(padding))
            is SearchState.Error   -> ErrorView(state.message, viewModel::retry,
                Modifier.padding(padding))
            is SearchState.Success -> {
                if (category == SearchCategory.IMAGES) {
                    // ── 2-column image grid ───────────────────────────────────
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(2),
                        state                 = gridState,
                        modifier              = Modifier.padding(padding),
                        contentPadding        = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ResultStats(state.response.numberOfResults, state.instanceUrl)
                        }
                        if (state.response.answers.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AnswerBox(state.response.answers.first())
                            }
                        }
                        // Key uses index + url to guarantee uniqueness even when
                        // multiple engines return the same image URL (duplicate-key
                        // crash fix: Compose throws IllegalArgumentException on dupe keys)
                        itemsIndexed(
                            items = state.response.results,
                            key   = { index, item -> "${index}_${item.url}" },
                        ) { _, result ->
                            ImageThumbnailCard(result = result)
                        }
                        if (state.response.results.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LoadMoreButton(onClick = viewModel::loadNextPage)
                            }
                        }
                        if (state.response.suggestions.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SuggestionsBar(state.response.suggestions) { viewModel.search(it) }
                            }
                        }
                    }
                } else {
                    // ── Standard list ─────────────────────────────────────────
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.padding(padding),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item { ResultStats(state.response.numberOfResults, state.instanceUrl) }
                        if (state.response.answers.isNotEmpty()) {
                            item { AnswerBox(state.response.answers.first()) }
                        }
                        // Same index+url composite key here for safety
                        itemsIndexed(
                            items = state.response.results,
                            key   = { index, item -> "${index}_${item.url}" },
                        ) { _, result ->
                            ResultCard(result = result)
                        }
                        if (state.response.results.isNotEmpty()) {
                            item { LoadMoreButton(onClick = viewModel::loadNextPage) }
                        }
                        if (state.response.suggestions.isNotEmpty()) {
                            item {
                                SuggestionsBar(state.response.suggestions) { viewModel.search(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Private composables ──────────────────────────────────────────────────────

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text("Searching privately...", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.WifiOff, null, modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Search failed", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary)) { Text("Retry") }
        }
    }
}

@Composable
private fun ResultStats(count: Long, instanceUrl: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(if (count > 0) "~${formatCount(count)} results" else "Results",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.secondary)
                Text(instanceUrl.removePrefix("https://").removePrefix("http://").take(24),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AnswerBox(answer: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Lightbulb, null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(answer, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun LoadMoreButton(onClick: () -> Unit) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text("Load more results", color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SuggestionsBar(suggestions: List<String>, onSuggestion: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Related searches", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions.take(6)) { s ->
                SuggestionChip(onClick = { onSuggestion(s) },
                    label = { Text(s, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}

private fun formatCount(n: Long): String = when {
    n >= 1_000_000_000 -> "${n / 1_000_000_000}B"
    n >= 1_000_000     -> "${n / 1_000_000}M"
    n >= 1_000         -> "${n / 1_000}K"
    else               -> n.toString()
}
