package com.vairagi.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vairagi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsState()
    var showResetSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Group 1: Reminders & Goals
            SectionHeader(title = "Reminders & Intentions")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StandardCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SliderSettingRow(
                        icon = Icons.Default.Timer,
                        title = "Continuous Streak Target",
                        subtitle = "Alert after ${settings.continuousIntervalMinutes} minutes of non-stop scrolling",
                        value = settings.continuousIntervalMinutes.toFloat(),
                        valueRange = 5f..60f,
                        steps = 10,
                        valueLabel = "${settings.continuousIntervalMinutes}m",
                        onValueChange = { viewModel.updateContinuousInterval(it.toInt()) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))
                    SliderSettingRow(
                        icon = Icons.Default.Schedule,
                        title = "Cumulative Daily Intention",
                        subtitle = "Alert after ${settings.cumulativeIntervalMinutes} minutes total screen time",
                        value = settings.cumulativeIntervalMinutes.toFloat(),
                        valueRange = 15f..300f,
                        steps = 18,
                        valueLabel = "${settings.cumulativeIntervalMinutes}m",
                        onValueChange = { viewModel.updateCumulativeInterval(it.toInt()) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))
                    SliderSettingRow(
                        icon = Icons.Default.PauseCircle,
                        title = "Screen-Off Reset Threshold",
                        subtitle = "Reset continuous streak if screen stays off for ${settings.breakThresholdSeconds} seconds",
                        value = settings.breakThresholdSeconds.toFloat(),
                        valueRange = 15f..180f,
                        steps = 10,
                        valueLabel = "${settings.breakThresholdSeconds}s",
                        onValueChange = { viewModel.updateBreakThreshold(it.toInt()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 2: Break Behavior & Sound
            SectionHeader(title = "Break Experience")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StandardCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleSettingRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Mindful Chime Sound",
                        subtitle = "Play a soft audio chime when break overlay appears",
                        checked = settings.soundEnabled,
                        onCheckedChange = { viewModel.updateSoundEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 3: Appearance
            SectionHeader(title = "Appearance")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StandardCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleSettingRow(
                        icon = Icons.Default.Palette,
                        title = "Material You Dynamic Colors",
                        subtitle = "Adapt color theme to device wallpaper (Android 12+)",
                        checked = settings.useDynamicColor,
                        onCheckedChange = { viewModel.updateDynamicColor(it) }
                    )
                }
            }

            // Group 4: Privacy & Security
            SectionHeader(title = "Privacy & Permission Audit")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StandardCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = LivingLeaf)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("On-Device Privacy Guarantee", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vairagi collects 0 bytes of external analytics. All usage timestamps, durations, and preferences remain 100% on your local device.",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionAuditRow(
                        title = "Usage Access (PACKAGE_USAGE_STATS)",
                        description = "Strictly used on-device to query today's foreground app durations."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionAuditRow(
                        title = "System Overlay (SYSTEM_ALERT_WINDOW)",
                        description = "Strictly used to render full-screen mindful break popups."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionAuditRow(
                        title = "Autostart (RECEIVE_BOOT_COMPLETED)",
                        description = "Strictly used to re-enable continuous tracking after device reboot."
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleSettingRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hide Sensitive Data in Recents",
                        subtitle = "Obscure Dashboard in app-switcher thumbnail & block screenshots",
                        checked = settings.hideSensitiveDataInRecents,
                        onCheckedChange = { viewModel.updateHideSensitiveData(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
                    ToggleSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Require Biometric / PIN for Pausing",
                        subtitle = "Confirm fingerprint or device PIN before allowing tracking pause",
                        checked = settings.requireBiometricToPause,
                        onCheckedChange = { viewModel.updateRequireBiometric(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 5: Data & Reset
            SectionHeader(title = "Data & Export")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StandardCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ActionSettingRow(
                        icon = Icons.Default.FileDownload,
                        title = "Export Usage History (CSV)",
                        subtitle = "Share 7-day screen time logs to a CSV spreadsheet file",
                        onClick = { viewModel.exportDataCsv() }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                    ActionSettingRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Reset Vairagi",
                        subtitle = "Clear screen time stats and preferences",
                        tintColor = MutedTerracotta,
                        onClick = { showResetSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Confirmation Sheet for Reset Vairagi
        if (showResetSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResetSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MutedTerracotta,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reset All Vairagi Data?",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This action will permanently erase your 7-day usage history and reset your intention goals to defaults.",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.resetAllData {
                                showResetSheet = false
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = ComponentShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MutedTerracotta, contentColor = Color.White)
                    ) {
                        Text("Erase Everything", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showResetSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", style = Typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = Typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = LivingLeaf,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SliderSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ComponentShape)
                    .background(LivingLeaf.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = LivingLeaf, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = LivingLeaf.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = Typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LivingLeaf
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LivingLeaf,
                activeTrackColor = LivingLeaf,
                inactiveTrackColor = SageLight
            )
        )
    }
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ComponentShape)
                .background(LivingLeaf.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = LivingLeaf, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmParchment,
                checkedTrackColor = LivingLeaf,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                uncheckedTrackColor = SageLight
            )
        )
    }
}

@Composable
private fun ActionSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tintColor: Color = LivingLeaf,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ComponentShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ComponentShape)
                .background(tintColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = if (tintColor == MutedTerracotta) MutedTerracotta else MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun PermissionAuditRow(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = LivingLeaf,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
