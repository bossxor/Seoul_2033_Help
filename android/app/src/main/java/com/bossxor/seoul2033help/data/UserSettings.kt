package com.bossxor.seoul2033help.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSettings(
    val overlayAlpha: Float = 0.85f,
    val autoOcr: Boolean = false,
    val autoOcrIntervalSec: Int = 3,
    val cropBottomFraction: Float = 0.45f,
    val showRecommend: Boolean = true
) {
    fun clamped() = copy(
        overlayAlpha = overlayAlpha.coerceIn(ALPHA_RANGE),
        cropBottomFraction = cropBottomFraction.coerceIn(CROP_RANGE),
        autoOcrIntervalSec = autoOcrIntervalSec.coerceIn(INTERVAL_RANGE)
    )

    companion object {
        val ALPHA_RANGE = 0.4f..0.95f
        val CROP_RANGE = 0.25f..0.7f
        val INTERVAL_RANGE = 1..10
    }
}

private val Context.dataStore by preferencesDataStore("user_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val alpha = floatPreferencesKey("overlayAlpha")
        val auto = booleanPreferencesKey("autoOcr")
        val interval = intPreferencesKey("autoOcrIntervalSec")
        val crop = floatPreferencesKey("cropBottomFraction")
        val recommend = booleanPreferencesKey("showRecommend")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings()).clamped()
            p[K.alpha] = s.overlayAlpha
            p[K.auto] = s.autoOcr
            p[K.interval] = s.autoOcrIntervalSec
            p[K.crop] = s.cropBottomFraction
            p[K.recommend] = s.showRecommend
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            this[K.alpha] ?: d.overlayAlpha,
            this[K.auto] ?: d.autoOcr,
            this[K.interval] ?: d.autoOcrIntervalSec,
            this[K.crop] ?: d.cropBottomFraction,
            this[K.recommend] ?: d.showRecommend
        ).clamped()
    }
}
