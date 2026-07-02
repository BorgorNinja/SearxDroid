package dev.searxdroid.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.searxdroid.app.data.model.SearxResult

// ─────────────────────────────────────────────────────────────────────────────
//  SearxSearchBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearxSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.outlineVariant,
        label = "border",
    )

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(if (compact) 8.dp else 12.dp))
            .clip(RoundedCornerShape(if (compact) 8.dp else 12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            TextField(
                value         = query,
                onValueChange = onQueryChange,
                modifier      = Modifier
                    .weight(1f)
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                        else Modifier
                    )
                    .onFocusChanged { focused = it.isFocused },
                placeholder = {
                    Text(
                        "Search the web privately...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                singleLine  = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent,
                ),
            )
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ResultCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ResultCard(result: SearxResult, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hovered by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (hovered) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.outlineVariant,
        label = "border",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                context.startActivity(intent)
            },
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            // We can't easily animate card border here, so use static outline
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Domain + favicon row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                FaviconImage(domain = result.displayDomain)
                Text(
                    text  = result.displayDomain,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }

            // Title
            Text(
                text  = result.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow  = TextOverflow.Ellipsis,
            )

            // Snippet
            if (result.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = result.content,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Source chips
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                if (result.engine.isNotBlank()) {
                    PrivacyChip(icon = Icons.Outlined.Source, label = result.engine.replaceFirstChar { it.titlecase() })
                }
                if (result.category.isNotBlank() && result.category != "general") {
                    PrivacyChip(icon = Icons.Outlined.Category, label = result.category.replaceFirstChar { it.titlecase() })
                }
                if (result.publishedDate != null) {
                    PrivacyChip(icon = Icons.Outlined.Schedule, label = result.publishedDate.take(10))
                }
            }
        }
    }
}

@Composable
private fun FaviconImage(domain: String) {
    val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=32"
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model   = ImageRequest.Builder(LocalContext.current)
                .data(faviconUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CategoryChip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = CircleShape,
        color    = if (selected) MaterialTheme.colorScheme.secondaryContainer
                   else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PrivacyChip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrivacyChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Needed for AnimatedVisibility import
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun AnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(visible = visible) { content() }
}
