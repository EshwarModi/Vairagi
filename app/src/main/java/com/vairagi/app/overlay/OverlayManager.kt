package com.vairagi.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vairagi.app.engine.TriggerReason
import com.vairagi.app.sound.SoundPlayer
import com.vairagi.app.ui.theme.*
import kotlinx.coroutines.delay

class OverlayManager(
    private val context: Context,
    private val onDismissContinuous: () -> Unit
) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val soundPlayer = SoundPlayer(context)

    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = overlayView != null

    fun showOverlay(reason: TriggerReason, soundEnabled: Boolean) {
        if (!Settings.canDrawOverlays(context)) {
            return
        }

        if (overlayView != null) {
            removeOverlay()
        }

        soundPlayer.playChime(soundEnabled)

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                VairagiTheme(darkTheme = true) {
                    OverlayContent(
                        reason = reason,
                        onDismiss = {
                            removeOverlay()
                            if (reason is TriggerReason.Continuous || reason is TriggerReason.Both) {
                                onDismissContinuous()
                            }
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }
}

@Composable
fun OverlayContent(
    reason: TriggerReason,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var isBreathingMode by remember { mutableStateOf(false) }
    var breakTimer by remember { mutableIntStateOf(60) }

    LaunchedEffect(isBreathingMode) {
        if (isBreathingMode) {
            while (breakTimer > 0) {
                delay(1000L)
                breakTimer--
            }
            onDismiss()
        }
    }

    // Slow-pulsing breathing animation (4s inhale / 4s exhale)
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingCircle")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(24.dp)
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ForestSage.copy(alpha = 0.95f),
                        WarmCharcoal.copy(alpha = 0.98f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Breathing Circle Core Animation
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft Outer Aura Ring
                Canvas(modifier = Modifier.size(180.dp * breathingScale)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                FreshSprout.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                }

                // Center Inner Breathing Orb
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(LivingLeaf.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = FreshSprout,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Calm, Non-judgmental Headline
            Text(
                text = if (isBreathingMode) "Inhale... Exhale..." else "You've been present here a while",
                style = Typography.displaySmall,
                color = WarmParchment,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isBreathingMode) {
                Text(
                    text = "${breakTimer}s mindful pause",
                    style = Typography.displayLarge,
                    color = FreshSprout,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmParchment)
                ) {
                    Text("Complete Break")
                }
            } else {
                // Reason Badge
                val reasonText = when (reason) {
                    is TriggerReason.Continuous -> "${reason.minutes} min continuous awareness"
                    is TriggerReason.Cumulative -> "${reason.minutes} min daily total"
                    is TriggerReason.Both -> "${reason.continuousMinutes}m streak • ${reason.cumulativeMinutes}m total"
                }

                Surface(
                    color = SageLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = FreshSprout,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reasonText,
                            style = Typography.labelMedium,
                            color = FreshSprout,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A gentle moment to detach from the screen, relax your shoulders, and rest your eyes.",
                    style = Typography.bodyLarge,
                    color = WarmParchment.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Primary Action Pill Button ("Take a breath")
                Button(
                    onClick = { isBreathingMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshSprout,
                        contentColor = ForestSage
                    )
                ) {
                    Text(
                        text = "Take a breath (1 min)",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // De-emphasized Secondary Button ("5 more minutes") - Intentional Friction
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue for 5 more minutes",
                        style = Typography.labelLarge,
                        color = WarmParchment.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
