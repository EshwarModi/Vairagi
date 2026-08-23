package com.vairagi.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.vairagi.app.MainActivity
import com.vairagi.app.R
import com.vairagi.app.engine.UsageStatsHelper

class AppUsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_app_usage)

                val helper = UsageStatsHelper(context)
                val topApps = helper.getTodayAppUsageList()

                // Calculate total screen time across apps
                val totalSeconds = topApps.sumOf { it.usageSeconds }
                views.setTextViewText(R.id.widget_total_screen_time, formatSeconds(totalSeconds))

                // App 1
                if (topApps.isNotEmpty()) {
                    val app1 = topApps[0]
                    views.setViewVisibility(R.id.widget_app1_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_app1_name, app1.appName)
                    views.setTextViewText(R.id.widget_app1_time, formatSeconds(app1.usageSeconds))
                    if (app1.iconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_app1_icon, app1.iconBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_app1_icon, R.drawable.ic_launcher_foreground)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_app1_container, View.GONE)
                }

                // App 2
                if (topApps.size > 1) {
                    val app2 = topApps[1]
                    views.setViewVisibility(R.id.widget_app2_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_app2_name, app2.appName)
                    views.setTextViewText(R.id.widget_app2_time, formatSeconds(app2.usageSeconds))
                    if (app2.iconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_app2_icon, app2.iconBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_app2_icon, R.drawable.ic_launcher_foreground)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_app2_container, View.GONE)
                }

                // App 3
                if (topApps.size > 2) {
                    val app3 = topApps[2]
                    views.setViewVisibility(R.id.widget_app3_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_app3_name, app3.appName)
                    views.setTextViewText(R.id.widget_app3_time, formatSeconds(app3.usageSeconds))
                    if (app3.iconBitmap != null) {
                        views.setImageViewBitmap(R.id.widget_app3_icon, app3.iconBitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_app3_icon, R.drawable.ic_launcher_foreground)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_app3_container, View.GONE)
                }

                // Tap widget to open Vairagi MainActivity
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Throwable) {
                Log.e("AppUsageWidget", "Error updating widget", e)
            }
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AppUsageWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Throwable) {
                Log.e("AppUsageWidget", "Error updating all widgets", e)
            }
        }

        private fun formatSeconds(seconds: Long): String {
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            return when {
                hrs > 0 -> "${hrs}h ${mins}m"
                mins > 0 -> "${mins}m"
                else -> "<1m"
            }
        }
    }
}
