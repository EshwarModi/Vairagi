package com.vairagi.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vairagi.app.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vairagi_settings")

data class AppSettings(
    val soundEnabled: Boolean = true,
    val continuousIntervalMinutes: Int = 15,
    val cumulativeIntervalMinutes: Int = 30,
    val breakThresholdSeconds: Int = 60,
    val trackingPaused: Boolean = false,
    val useDynamicColor: Boolean = false,
    val hideSensitiveDataInRecents: Boolean = true,
    val showAppNamesOnWidget: Boolean = false,
    val requireBiometricToPause: Boolean = false
)

data class DailyUsageStats(
    val cumulativeSecondsToday: Long = 0L,
    val continuousSecondsStreak: Long = 0L,
    val lastMidnightTimestamp: Long = 0L,
    val history7Days: Map<String, Long> = emptyMap()
)

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_CONTINUOUS_INTERVAL = intPreferencesKey("continuous_interval_minutes")
        val KEY_CUMULATIVE_INTERVAL = intPreferencesKey("cumulative_interval_minutes")
        val KEY_BREAK_THRESHOLD = intPreferencesKey("break_threshold_seconds")
        val KEY_TRACKING_PAUSED = booleanPreferencesKey("tracking_paused")
        val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_HIDE_RECENTS = booleanPreferencesKey("hide_sensitive_data_in_recents")
        val KEY_SHOW_WIDGET_APP_NAMES = booleanPreferencesKey("show_app_names_on_widget")
        val KEY_REQUIRE_BIOMETRIC = booleanPreferencesKey("require_biometric_to_pause")

        val KEY_CUMULATIVE_TODAY = longPreferencesKey("cumulative_seconds_today")
        val KEY_CONTINUOUS_STREAK = longPreferencesKey("continuous_seconds_streak")
        val KEY_LAST_MIDNIGHT_TS = longPreferencesKey("last_midnight_ts")
        val KEY_HISTORY_RAW = stringPreferencesKey("history_raw_data_enc")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            continuousIntervalMinutes = prefs[KEY_CONTINUOUS_INTERVAL] ?: 15,
            cumulativeIntervalMinutes = prefs[KEY_CUMULATIVE_INTERVAL] ?: 30,
            breakThresholdSeconds = prefs[KEY_BREAK_THRESHOLD] ?: 60,
            trackingPaused = prefs[KEY_TRACKING_PAUSED] ?: false,
            useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: false,
            hideSensitiveDataInRecents = prefs[KEY_HIDE_RECENTS] ?: true,
            showAppNamesOnWidget = prefs[KEY_SHOW_WIDGET_APP_NAMES] ?: false,
            requireBiometricToPause = prefs[KEY_REQUIRE_BIOMETRIC] ?: false
        )
    }

    val usageStatsFlow: Flow<DailyUsageStats> = context.dataStore.data.map { prefs ->
        val rawEncrypted = prefs[KEY_HISTORY_RAW] ?: ""
        val decryptedRaw = decryptString(rawEncrypted)
        val historyMap = parseHistory(decryptedRaw)
        DailyUsageStats(
            cumulativeSecondsToday = prefs[KEY_CUMULATIVE_TODAY] ?: 0L,
            continuousSecondsStreak = prefs[KEY_CONTINUOUS_STREAK] ?: 0L,
            lastMidnightTimestamp = prefs[KEY_LAST_MIDNIGHT_TS] ?: 0L,
            history7Days = historyMap
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
                continuousIntervalMinutes = prefs[KEY_CONTINUOUS_INTERVAL] ?: 15,
                cumulativeIntervalMinutes = prefs[KEY_CUMULATIVE_INTERVAL] ?: 30,
                breakThresholdSeconds = prefs[KEY_BREAK_THRESHOLD] ?: 60,
                trackingPaused = prefs[KEY_TRACKING_PAUSED] ?: false,
                useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: false,
                hideSensitiveDataInRecents = prefs[KEY_HIDE_RECENTS] ?: true,
                showAppNamesOnWidget = prefs[KEY_SHOW_WIDGET_APP_NAMES] ?: false,
                requireBiometricToPause = prefs[KEY_REQUIRE_BIOMETRIC] ?: false
            )
            val updated = transform(current)
            prefs[KEY_SOUND_ENABLED] = updated.soundEnabled
            prefs[KEY_CONTINUOUS_INTERVAL] = updated.continuousIntervalMinutes
            prefs[KEY_CUMULATIVE_INTERVAL] = updated.cumulativeIntervalMinutes
            prefs[KEY_BREAK_THRESHOLD] = updated.breakThresholdSeconds
            prefs[KEY_TRACKING_PAUSED] = updated.trackingPaused
            prefs[KEY_USE_DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[KEY_HIDE_RECENTS] = updated.hideSensitiveDataInRecents
            prefs[KEY_SHOW_WIDGET_APP_NAMES] = updated.showAppNamesOnWidget
            prefs[KEY_REQUIRE_BIOMETRIC] = updated.requireBiometricToPause
        }
    }

    suspend fun resetAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveUsageState(
        cumulativeSeconds: Long,
        continuousSeconds: Long,
        lastMidnightTs: Long,
        todayDateStr: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUMULATIVE_TODAY] = cumulativeSeconds
            prefs[KEY_CONTINUOUS_STREAK] = continuousSeconds
            prefs[KEY_LAST_MIDNIGHT_TS] = lastMidnightTs

            val decrypted = decryptString(prefs[KEY_HISTORY_RAW] ?: "")
            val history = parseHistory(decrypted).toMutableMap()
            history[todayDateStr] = cumulativeSeconds
            prefs[KEY_HISTORY_RAW] = encryptString(encodeHistory(history))
        }
    }

    private fun parseHistory(raw: String): Map<String, Long> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val date = parts[0]
                val sec = parts[1].toLongOrNull()
                if (sec != null) date to sec else null
            } else null
        }.toMap()
    }

    private fun encodeHistory(map: Map<String, Long>): String {
        return map.entries.toList().takeLast(14).joinToString(";") { "${it.key}=${it.value}" }
    }

    private fun encryptString(input: String): String {
        return try {
            HistoryCrypto.encrypt(input)
        } catch (e: Exception) {
            Logger.e("PreferencesManager", "History encryption failed", e)
            ""
        }
    }

    private fun decryptString(input: String): String {
        if (input.isBlank()) return ""
        // Primary path: real AES-256-GCM, key held in the Android Keystore.
        HistoryCrypto.decrypt(input)?.let { return it }
        // Fallback for data written before this app moved off the old XOR
        // "obfuscation" scheme — lets existing users' history survive the
        // upgrade instead of being silently wiped. The very next
        // saveUsageState() call re-encrypts it with AES-GCM, so this path
        // is only ever hit once per install.
        val legacy = HistoryCrypto.legacyXorDecode(input)
        if (legacy.isNotBlank()) {
            Logger.i("PreferencesManager", "Migrated legacy-obfuscated history to AES-GCM")
        }
        return legacy
    }
}

/**
 * AES-256-GCM encryption for on-disk usage history, backed by a key that
 * never leaves the Android Keystore (hardware-backed on most devices).
 *
 * Ciphertext layout (before Base64): [12-byte GCM IV][ciphertext + 16-byte tag]
 */
private object HistoryCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vairagi_history_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherBytes, Base64.NO_WRAP)
    }

    /** Returns null (rather than throwing) on any failure, so callers can fall back. */
    fun decrypt(cipherText: String): String? {
        if (cipherText.isBlank()) return ""
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH_BYTES) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes data written by the pre-hardening build, which only XORed
     * bytes against a fixed constant and Base64-encoded them — not real
     * encryption, just obfuscation. Used solely as a one-time migration
     * fallback in [PreferencesManager.decryptString]; never used to encrypt.
     */
    fun legacyXorDecode(input: String): String {
        if (input.isBlank()) return ""
        return try {
            val bytes = Base64.decode(input, Base64.NO_WRAP)
            val deobfuscated = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor 0x5A).toByte() }
            String(deobfuscated, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
