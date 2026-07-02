package dev.searxdroid.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.searxdroid.app.data.model.SearchCategory
import dev.searxdroid.app.ui.components.CategoryChip
import dev.searxdroid.app.ui.components.SearxSearchBar

@Composable
fun HomeScreen(
    initialQuery: String = "",
    onSearch: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableStateOf(SearchCategory.GENERAL) }
    val focusRequester = remember { FocusRequester() }

    // Ambient pulse animation for background blobs
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ), label = "pulse",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Security, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("SearxDroid", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Ambient background blobs (matches search_dashboard.html animated blobs)
            Box(modifier = Modifier
                .size(240.dp)
                .offset(x = (-40).dp, y = (-60).dp)
                .blur(80.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = pulseAlpha),
                    CircleShape,
                )
            )
            Box(modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .blur(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.7f),
                    CircleShape,
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {

                // ── Shield + headline ─────────────────────────────────────
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Privacy Search",
                    style     = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Search without tracking. Results from 70+ engines across the web.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))

                // ── Search bar ────────────────────────────────────────────
                SearxSearchBar(
                    query          = query,
                    onQueryChange  = { query = it },
                    onSearch       = { if (query.isNotBlank()) onSearch(query) },
                    focusRequester = focusRequester,
                    modifier       = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                // ── Category chips ────────────────────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(horizontal = 0.dp),
                ) {
                    items(SearchCategory.entries) { cat ->
                        CategoryChip(
                            label    = cat.label,
                            selected = cat == selectedCategory,
                            onClick  = { selectedCategory = cat },
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))

                // ── Privacy status card ───────────────────────────────────
                PrivacyStatusCard()
            }
        }
    }

    // Auto-focus search bar if we arrived with an initial query
    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) onSearch(initialQuery)
    }
}

@Composable
private fun PrivacyStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border   = CardDefaults.outlinedCardBorder(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.VerifiedUser, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Privacy Status", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Surface(
                        shape  = RoundedCornerShape(4.dp),
                        color  = Color(0xFF166534).copy(alpha = 0.15f),
                    ) {
                        Text("ACTIVE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your queries are proxied through encrypted instances. No fingerprints or cookies stored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrivacyBadge(icon = Icons.Outlined.Lock, label = "SSL Encrypted")
                    PrivacyBadge(icon = Icons.Outlined.VisibilityOff, label = "No Tracking")
                }
            }
        }
    }
}

@Composable
private fun PrivacyBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
