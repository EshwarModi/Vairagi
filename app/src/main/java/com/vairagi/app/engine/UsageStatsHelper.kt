package com.vairagi.app.engine

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.Calendar

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usageSeconds: Long,
    val iconBitmap: Bitmap?
)

class UsageStatsHelper(private val context: Context) {

    fun getTodayAppUsageList(): List<AppUsageItem> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        if (statsMap.isNullOrEmpty()) return emptyList()

        val pm = context.packageManager
        val mainLauncherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val launcherPackage = pm.resolveActivity(mainLauncherIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

        val resultList = mutableListOf<AppUsageItem>()

        for ((pkgName, usageStats) in statsMap) {
            val totalSeconds = usageStats.totalTimeInForeground / 1000L
            if (totalSeconds < 10L) continue // Skip under 10 seconds

            // Filter out system launchers, system UI, and Vairagi itself
            if (pkgName == context.packageName || pkgName == launcherPackage || pkgName == "com.android.systemui") {
                continue
            }

            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                // Filter out non-launcher system apps without a launcher activity
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent == null) continue

                val appName = pm.getApplicationLabel(appInfo).toString()
                val iconDrawable = pm.getApplicationIcon(appInfo)
                val iconBitmap = drawableToBitmap(iconDrawable)

                resultList.add(
                    AppUsageItem(
                        packageName = pkgName,
                        appName = appName,
                        usageSeconds = totalSeconds,
                        iconBitmap = iconBitmap
                    )
                )
            } catch (e: Exception) {
                // Application uninstalled or inaccessible
            }
        }

        return resultList.sortedByDescending { it.usageSeconds }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val targetSize = 64 // 64x64 px for safe RemoteViews IPC
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
