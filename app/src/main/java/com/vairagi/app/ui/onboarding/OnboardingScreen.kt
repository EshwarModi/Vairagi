package com.vairagi.app.ui.onboarding

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vairagi.app.data.PreferencesManager
import com.vairagi.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    var hasUsageStats by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = checkUsageStatsPermission(context)
                hasOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var selectedIntentionMinutes by remember { mutableFloatStateOf(120f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    if (pagerState.currentPage < pageCount - 1) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(pageCount - 1) }
                            }
                        ) {
                            Text("Skip", style = Typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Leaf Motif Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) LivingLeaf else SageLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = WarmParchment,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Button
                if (pagerState.currentPage < pageCount - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = HeroCardShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestSage, contentColor = WarmParchment)
                    ) {
                        Text("Continue", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (hasUsageStats && hasOverlay) {
                                coroutineScope.launch {
                                    preferencesManager.updateSettings { it.copy(cumulativeIntervalMinutes = selectedIntentionMinutes.toInt()) }
                                    onOnboardingComplete()
                                }
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = HeroCardShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUsageStats && hasOverlay) LivingLeaf else ForestSage,
                            contentColor = WarmParchment
                        )
                    ) {
                        Text(
                            text = if (hasUsageStats && hasOverlay) "Begin Mindful Journey" else "Grant Required Permissions",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> PageBrandMoment()
                1 -> PagePhilosophy()
                2 -> PagePermissions(
                    hasUsageStats = hasUsageStats,
                    hasOverlay = hasOverlay,
                    onGrantUsage = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    onGrantOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
                3 -> PageOemBackgroundProtection()
                4 -> PageDailyIntention(
                    intentionMinutes = selectedIntentionMinutes,
                    onIntentionChange = { selectedIntentionMinutes = it }
                )
            }
        }
    }
}

@Composable
private fun PageBrandMoment() {
    val transition = rememberInfiniteTransition(label = "LeafDrift")
    val driftY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val branchPath = Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.2f)
                cubicTo(size.width * 0.4f, size.height * 0.3f, size.width * 0.6f, size.height * 0.35f, size.width * 0.7f, size.height * 0.4f)
            }
            drawPath(branchPath, ForestSage, style = Stroke(width = 4.dp.toPx()))

            val leafCenter = Offset(size.width * 0.65f, size.height * 0.45f + driftY)
            drawCircle(
                brush = Brush.radialGradient(colors = listOf(FreshSprout.copy(alpha = 0.3f), Color.Transparent)),
                center = leafCenter,
                radius = 48.dp.toPx()
            )
            drawCircle(LivingLeaf, radius = 24.dp.toPx(), center = leafCenter)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Vairagi",
            style = Typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "The art of letting go",
            style = Typography.headlineSmall,
            color = LivingLeaf,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mindful screen detachment through gentle awareness, unhurried breaks, and positive intention.",
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun PagePhilosophy() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "How Vairagi Protects Focus",
            style = Typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Two gentle guardrails designed to prevent digital fatigue",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = StandardCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ComponentShape)
                        .background(FreshSprout.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = LivingLeaf)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Continuous Session Streak", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Interrupts continuous 15-minute scrolling streaks. Turning off your screen for 60s resets the streak automatically.",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = StandardCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ComponentShape)
                        .background(WarmUmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = WarmUmber)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Cumulative Daily Intention", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Monitors total daily screen time from midnight. Encourages mindful pauses as you approach your goal.",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PagePermissions(
    hasUsageStats: Boolean,
    hasOverlay: Boolean,
    onGrantUsage: () -> Unit,
    onGrantOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Required Access",
            style = Typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vairagi operates entirely on-device with zero data sharing",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        PermissionCard(
            title = "Usage Access",
            description = "Needed to aggregate screen time and per-app usage stats for your widget.",
            icon = Icons.Default.Analytics,
            isGranted = hasUsageStats,
            onGrantClick = onGrantUsage
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            title = "Display Over Other Apps",
            description = "Needed to render serene break overlays during continuous scrolling.",
            icon = Icons.Default.Layers,
            isGranted = hasOverlay,
            onGrantClick = onGrantOverlay
        )
    }
}

@Composable
private fun PageOemBackgroundProtection() {
    val context = LocalContext.current
    val manufacturer = remember { Build.MANUFACTURER.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Uninterrupted Protection",
            style = Typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Detected $manufacturer Device",
            style = Typography.titleMedium,
            color = LivingLeaf,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Some device manufacturers aggressively terminate background services unless autostart is permitted.",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = StandardCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BatterySaver, contentDescription = null, tint = LivingLeaf)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Allow Autostart / Unrestricted Battery", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ensures Vairagi's gentle mindful streak tracking stays active smoothly in the background.",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = getOemAutostartIntent(context) ?: Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = LivingLeaf)
                ) {
                    Text("Configure Background Settings")
                }
            }
        }
    }
}

@Composable
private fun PageDailyIntention(
    intentionMinutes: Float,
    onIntentionChange: (Float) -> Unit
) {
    val hours = (intentionMinutes / 60f).toInt()
    val mins = (intentionMinutes % 60f).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set Your Daily Intention",
            style = Typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "How much total screen time feels balanced today?",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = HeroCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    style = Typography.displayLarge,
                    color = LivingLeaf,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recommended target for healthy detachment",
                    style = Typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Slider(
                    value = intentionMinutes,
                    onValueChange = onIntentionChange,
                    valueRange = 30f..300f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = LivingLeaf,
                        activeTrackColor = LivingLeaf,
                        inactiveTrackColor = SageLight
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("30m", style = Typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("5h", style = Typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StandardCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(ComponentShape)
                    .background(LivingLeaf.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = LivingLeaf)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            AnimatedVisibility(
                visible = isGranted,
                enter = fadeIn() + scaleIn()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = LivingLeaf,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (!isGranted) {
                Button(
                    onClick = onGrantClick,
                    shape = ComponentShape,
                    colors = ButtonDefaults.buttonColors(containerColor = LivingLeaf)
                ) {
                    Text("Grant", style = Typography.labelSmall)
                }
            }
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun getOemAutostartIntent(context: Context): Intent? {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
            Intent().apply { setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")) }
        }
        manufacturer.contains("oppo") -> {
            Intent().apply { setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")) }
        }
        manufacturer.contains("vivo") -> {
            Intent().apply { setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")) }
        }
        manufacturer.contains("huawei") -> {
            Intent().apply { setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")) }
        }
        manufacturer.contains("samsung") -> {
            Intent().apply { setComponent(ComponentName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity")) }
        }
        else -> null
    }
}
