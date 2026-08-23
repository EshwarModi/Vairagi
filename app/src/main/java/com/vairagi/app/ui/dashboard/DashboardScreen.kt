package com.vairagi.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vairagi.app.engine.AppUsageItem
import com.vairagi.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsState()
    val usageStats by viewModel.usageStatsState.collectAsState()
    val topApps by viewModel.topAppsState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val expandedAppPackage by viewModel.expandedAppPackage.collectAsState()
    val contextualInsight by viewModel.contextualInsightState.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refreshDashboard()
        }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullToRefreshState.endRefresh()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vairagi",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "The art of letting go",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.triggerTestOverlay() },
                containerColor = ForestSage,
                contentColor = FreshSprout,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(imageVector = Icons.Default.SelfImprovement, contentDescription = null) },
                text = { Text("Mindful Break", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Status & Contextual Insight Banner
                StatusInsightCard(
                    isPaused = settings.trackingPaused,
                    insight = contextualInsight,
                    onTogglePause = { viewModel.togglePauseTracking() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hero Circular Progress Ring
                HeroScreenTimeProgress(
                    cumulativeSeconds = usageStats.cumulativeSecondsToday,
                    intentionMinutes = settings.cumulativeIntervalMinutes,
                    continuousStreakSeconds = usageStats.continuousSecondsStreak
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 7-Day Leaf Streak Tracker
                LeafStreakTracker(
                    history7Days = usageStats.history7Days,
                    intentionMinutes = settings.cumulativeIntervalMinutes,
                    todayCumulative = usageStats.cumulativeSecondsToday
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Top Used Apps Card with Expandable Mini-Trends
                TopAppsBreakdownCard(
                    topApps = topApps,
                    totalScreenSeconds = usageStats.cumulativeSecondsToday,
                    expandedPackage = expandedAppPackage,
                    onAppClick = { pkg -> viewModel.toggleAppExpanded(pkg) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Animated 7-Day Usage Chart
                Usage7DaysBarChart(
                    historyData = usageStats.history7Days,
                    todayCumulative = usageStats.cumulativeSecondsToday,
                    intentionMinutes = settings.cumulativeIntervalMinutes
                )

                Spacer(modifier = Modifier.height(80.dp)) // Extra padding for FAB
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusInsightCard(
    isPaused: Boolean,
    insight: String,
    onTogglePause: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StandardCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) AmberClay else LivingLeaf)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPaused) "Tracking Suspended" else insight,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isPaused) "Tap play to resume monitoring" else "Continuous screen-time active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause / Resume",
                    tint = if (isPaused) LivingLeaf else AmberClay
                )
            }
        }
    }
}

@Composable
private fun HeroScreenTimeProgress(
    cumulativeSeconds: Long,
    intentionMinutes: Int,
    continuousStreakSeconds: Long
) {
    val targetProgress = remember(cumulativeSeconds, intentionMinutes) {
        val intentionSec = intentionMinutes * 60L
        if (intentionSec > 0) (cumulativeSeconds.toFloat() / intentionSec.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val animatedProgress = remember { Animatable(0f) }
    val animatedSeconds = remember { Animatable(0f) }

    LaunchedEffect(targetProgress, cumulativeSeconds) {
        animatedProgress.animateTo(targetProgress, animationSpec = tween(1200))
        animatedSeconds.animateTo(cumulativeSeconds.toFloat(), animationSpec = tween(1200))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = HeroCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft Radial Background Glow
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FreshSprout.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Circular Progress Ring
            Canvas(modifier = Modifier.size(220.dp)) {
                val strokeWidth = 18.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Background Track
                drawCircle(
                    color = SageLight.copy(alpha = 0.5f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                val sweepAngle = animatedProgress.value * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(LivingLeaf, FreshSprout, LivingLeaf)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Central Readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Screen Intention",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSecondsToHoursMinutes(animatedSeconds.value.toLong()),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = WarmUmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = WarmUmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatSecondsToHoursMinutes(continuousStreakSeconds)} streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmUmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeafStreakTracker(
    history7Days: Map<String, Long>,
    intentionMinutes: Int,
    todayCumulative: Long
) {
    val intentionSec = intentionMinutes * 60L
    val today = LocalDate.now()

    val daysList = remember(history7Days, todayCumulative, intentionMinutes) {
        (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.toString()
            val sec = if (daysAgo == 0) todayCumulative else (history7Days[dateStr] ?: 0L)
            val isSuccess = sec <= intentionSec
            val label = if (daysAgo == 0) "T" else date.dayOfWeek.name.take(1)
            Triple(label, isSuccess, daysAgo == 0)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StandardCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Mindful Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Goal: ${intentionMinutes}m/day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysList.forEach { (label, isSuccess, isToday) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSuccess) LivingLeaf.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.Eco else Icons.Default.Park,
                                contentDescription = null,
                                tint = if (isSuccess) LivingLeaf else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppsBreakdownCard(
    topApps: List<AppUsageItem>,
    totalScreenSeconds: Long,
    expandedPackage: String?,
    onAppClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StandardCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = LivingLeaf,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Top Used Apps Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = "Analyzing usage statistics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                topApps.take(4).forEachIndexed { index, app ->
                    val isExpanded = expandedPackage == app.packageName
                    val percent = if (totalScreenSeconds > 0) (app.usageSeconds.toFloat() / totalScreenSeconds.toFloat()).coerceIn(0f, 1f) else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(app.packageName) }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(ComponentShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = app.appName,
                                        tint = LivingLeaf,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Animated Thin Progress Bar
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = LivingLeaf,
                                        trackColor = SageLight.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Text(
                                text = formatSecondsToHoursMinutes(app.usageSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LivingLeaf
                            )
                        }

                        // Expandable 7-Day Mini Trend
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 50.dp)
                            ) {
                                Text(
                                    text = "7-Day Usage Trend for ${app.appName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Daily Avg: ${(app.usageSeconds / 60)}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LivingLeaf,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(percent * 100).toInt()}% of today's total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    if (index < topApps.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Usage7DaysBarChart(
    historyData: Map<String, Long>,
    todayCumulative: Long,
    intentionMinutes: Int
) {
    val last7Days: List<Pair<String, Long>> = remember(historyData, todayCumulative) {
        val today = LocalDate.now()
        (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.toString()
            val label = if (daysAgo == 0) "Today" else "${date.dayOfWeek.name.take(3)}"
            val seconds = if (daysAgo == 0) todayCumulative else (historyData[dateStr] ?: 0L)
            label to seconds
        }
    }

    val maxSeconds: Long = remember(last7Days, intentionMinutes) {
        val intentionSec = intentionMinutes * 60L
        (last7Days.maxOfOrNull { it.second } ?: intentionSec).coerceAtLeast(intentionSec)
    }

    val chartAnimation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        chartAnimation.animateTo(1f, animationSpec = tween(1000))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StandardCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = LivingLeaf
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val barWidth = size.width / (last7Days.size * 2.2f)
                val spacing = size.width / last7Days.size.toFloat()
                val intentionSec = intentionMinutes * 60L
                val intentionY = size.height - (size.height * (intentionSec.toFloat() / maxSeconds.toFloat()))

                // Dotted Average / Intention Line Overlay
                drawLine(
                    color = WarmUmber.copy(alpha = 0.5f),
                    start = Offset(0f, intentionY),
                    end = Offset(size.width, intentionY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Animated Gradient Bars
                last7Days.forEachIndexed { index, pair ->
                    val seconds = pair.second
                    val heightRatio = (seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.04f, 1.0f)
                    val fullBarHeight = size.height * heightRatio
                    val animatedBarHeight = fullBarHeight * chartAnimation.value
                    val x = (index.toFloat() * spacing) + (spacing / 2f) - (barWidth / 2f)
                    val y = size.height - animatedBarHeight

                    val isToday = index == last7Days.size - 1
                    val barBrush = if (isToday) {
                        Brush.verticalGradient(listOf(FreshSprout, LivingLeaf))
                    } else {
                        Brush.verticalGradient(listOf(LivingLeaf.copy(alpha = 0.7f), ForestSage))
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, animatedBarHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (pair in last7Days) {
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatSecondsToHoursMinutes(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
