# ProGuard keep rules for Vairagi

# Jetpack Compose
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Jetpack DataStore & Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * implements androidx.datastore.preferences.core.Preferences { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# ViewModel & Application classes
-keep class com.vairagi.app.VairagiApp { *; }
-keep class com.vairagi.app.data.AppSettings { *; }
-keep class com.vairagi.app.data.DailyUsageStats { *; }
-keep class com.vairagi.app.data.AppUsageItem { *; }
-keep class com.vairagi.app.ui.** { *; }
-keep class com.vairagi.app.engine.** { *; }
-keep class com.vairagi.app.service.** { *; }
-keep class com.vairagi.app.widget.** { *; }
