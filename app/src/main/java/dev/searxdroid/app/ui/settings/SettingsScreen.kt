package dev.searxdroid.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.searxdroid.app.data.model.SearxInstance
import dev.searxdroid.app.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context      = LocalContext.current
    val settings     = SettingsRepository.getInstance(context)
    val scope        = rememberCoroutineScope()

    val instanceUrl     by settings.instanceUrl.collectAsState(initial = "")
    val darkMode        by settings.darkMode.collectAsState(initial = false)
    val safeSearch      by settings.safeSearch.collectAsState(initial = 0)
    val language        by settings.language.collectAsState(initial = "en-US")
    val customInstances by settings.customInstances.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    val safeSearchLabels = listOf("Off", "Moderate", "Strict")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "Settings",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        LazyColumn(
            modifier            = Modifier.padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            // ── General ───────────────────────────────────────────────────
            item { SectionHeader("General") }
            item {
                SettingsGroup {
                    SettingsItem(
                        title    = "Language",
                        subtitle = language,
                        icon     = Icons.Outlined.Translate,
                        onClick  = { /* TODO */ },
                        trailing = {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsItem(
                        title    = "Theme",
                        subtitle = if (darkMode) "Dark" else "Light",
                        icon     = Icons.Outlined.DarkMode,
                        onClick  = { scope.launch { settings.setDarkMode(!darkMode) } },
                        trailing = {
                            Switch(
                                checked         = darkMode,
                                onCheckedChange = { scope.launch { settings.setDarkMode(it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                                ),
                            )
                        },
                    )
                }
            }

            // ── Privacy ───────────────────────────────────────────────────
            item { SectionHeader("Privacy") }
            item {
                SettingsGroup {
                    SettingsItem(
                        title    = "Safe Search",
                        subtitle = safeSearchLabels.getOrElse(safeSearch) { "Off" },
                        icon     = Icons.Outlined.ChildCare,
                        onClick  = {
                            scope.launch { settings.setSafeSearch((safeSearch + 1) % 3) }
                        },
                        trailing = {
                            Text(
                                text       = safeSearchLabels.getOrElse(safeSearch) { "Off" },
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
            }

            // ── Instances ─────────────────────────────────────────────────
            item { SectionHeader("Instances") }

            if (customInstances.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                        border   = CardDefaults.outlinedCardBorder(),
                    ) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "No instances configured",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Add a SearXNG instance URL to start searching. " +
                                "You can find public instances at searx.space, or " +
                                "host your own.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                item {
                    SettingsGroup {
                        customInstances.forEachIndexed { i, inst ->
                            InstanceRow(
                                instance = inst,
                                isActive = inst.url == instanceUrl,
                                onSelect = {
                                    scope.launch { settings.setInstanceUrl(inst.url) }
                                },
                                onDelete = {
                                    scope.launch {
                                        settings.removeCustomInstance(inst.url)
                                        // Clear active URL if the deleted instance was selected
                                        if (inst.url == instanceUrl) {
                                            settings.setInstanceUrl("")
                                        }
                                    }
                                },
                            )
                            if (i < customInstances.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.5f,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick  = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add instance")
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item { SectionHeader("About") }
            item {
                SettingsGroup {
                    SettingsItem(
                        title    = "SearxDroid",
                        subtitle = "Privacy-first SearXNG wrapper for Android",
                        icon     = Icons.Outlined.Info,
                        onClick  = {},
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsItem(
                        title    = "Source Code",
                        subtitle = "github.com/BorgorNinja/SearxDroid",
                        icon     = Icons.Outlined.Code,
                        onClick  = {},
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showAddDialog) {
        AddInstanceDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { inst ->
                scope.launch {
                    settings.addCustomInstance(inst)
                    settings.setInstanceUrl(inst.url)
                }
                showAddDialog = false
            },
        )
    }
}

// ─── Composable helpers ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title.uppercase(),
        modifier   = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp),
        style      = MaterialTheme.typography.labelSmall,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (subtitle.startsWith("http")) FontFamily.Monospace
                                 else FontFamily.Default,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun InstanceRow(
    instance: SearxInstance,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isActive,
            onClick  = onSelect,
            colors   = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                instance.name,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                instance.url,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (instance.country.isNotBlank()) {
            Text(
                instance.country,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddInstanceDialog(onDismiss: () -> Unit, onAdd: (SearxInstance) -> Unit) {
    var name  by remember { mutableStateOf("") }
    var url   by remember { mutableStateOf("https://") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Instance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the base URL of a SearXNG instance. " +
                    "Find public instances at searx.space.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = url,
                    onValueChange = { url = it; error = "" },
                    label         = { Text("URL") },
                    singleLine    = true,
                    isError       = error.isNotBlank(),
                    supportingText = if (error.isNotBlank()) {{ Text(error) }} else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction    = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!url.startsWith("http")) {
                    error = "URL must start with http:// or https://"
                    return@TextButton
                }
                onAdd(
                    SearxInstance(
                        name     = name.ifBlank {
                            url.removePrefix("https://").removePrefix("http://").trimEnd('/')
                        },
                        url      = url.trimEnd('/'),
                        isCustom = true,
                    )
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
