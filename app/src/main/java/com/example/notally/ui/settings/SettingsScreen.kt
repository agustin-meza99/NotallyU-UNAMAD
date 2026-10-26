package com.example.notally.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.ThemeMode
import com.example.notally.ui.components.OptionRow
import com.example.notally.ui.components.focusRing
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.NotallySpacing
import com.example.notally.ui.theme.screenHorizontalMargin
import com.example.notally.ui.util.DateFormat

/**
 * Pantalla Ajustes (§7.12).
 *
 * Cambiar el tema no reinicia la pantalla ni pierde el scroll: el estado del
 * `LazyColumn` y el `ViewModel` viven por encima del `NotallyTheme`.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(LocalContext.current),
    ),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val margin = screenHorizontalMargin
    val context = LocalContext.current
    var showThemeSheet by remember { mutableStateOf(false) }

    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = NotallySpacing.maxContentWidth)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = margin, end = margin, bottom = 32.dp),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.nav_ajustes),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .semantics { heading() },
                )
            }

            item(key = "appearance_label") {
                SectionLabel(stringResource(R.string.settings_appearance))
            }
            item(key = "theme_row") {
                SettingsRow(
                    label = stringResource(R.string.settings_theme),
                    value = themeLabel(settings.themeMode),
                    onClick = { showThemeSheet = true },
                )
            }
            item(key = "divider_appearance") { RowDivider() }
            item(key = "dynamic_row") {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_dynamic),
                    support = stringResource(
                        if (dynamicSupported) {
                            R.string.settings_dynamic_support
                        } else {
                            R.string.settings_dynamic_unsupported
                        },
                    ),
                    checked = settings.useDynamicColor && dynamicSupported,
                    enabled = dynamicSupported,
                    onCheckedChange = viewModel::setUseDynamicColor,
                )
            }

            item(key = "agenda_label") {
                SectionLabel(
                    stringResource(R.string.settings_agenda),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            item(key = "week_row") {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_week),
                    support = stringResource(R.string.settings_week_support),
                    checked = settings.weekStartsMonday,
                    enabled = true,
                    onCheckedChange = viewModel::setWeekStartsMonday,
                )
            }

            item(key = "about_label") {
                SectionLabel(
                    stringResource(R.string.settings_about),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            item(key = "version_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showThemeSheet) {
        ThemeSheet(
            current = settings.themeMode,
            onDismiss = { showThemeSheet = false },
            onSelect = { mode ->
                viewModel.setThemeMode(mode)
                showThemeSheet = false
            },
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(DateFormat.SpanishLocale),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    support: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRing(visible = focused, shape = NotallyShape.s)
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            if (support != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = support,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    support: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.38f
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRing(visible = focused, shape = NotallyShape.s)
            .onFocusChanged { focused = it.isFocused }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface.copy(alpha = alpha),
            )
            if (support != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = support,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // El switch no maneja el cambio: la fila completa es el target de 48 dp.
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.outline,
                uncheckedThumbColor = colors.outline,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    current: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = NotallyShape.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        )
        ThemeMode.entries.forEach { mode ->
            OptionRow(
                label = themeLabel(mode),
                selected = current == mode,
                onClick = { onSelect(mode) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_system
        ThemeMode.LIGHT -> R.string.settings_light
        ThemeMode.DARK -> R.string.settings_dark
    },
)
