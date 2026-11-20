package com.liquidwidgets.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.liquidwidgets.app.data.model.BackgroundMode
import com.liquidwidgets.app.data.model.LiquidFont
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.LiquidShape
import com.liquidwidgets.app.data.model.LiquidThemePreset
import com.liquidwidgets.app.data.model.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.liquidDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "liquid_widgets_prefs"
)

class WidgetConfigStore(private val context: Context) {

    private fun prefix(widgetType: WidgetType, appWidgetId: Int? = null): String {
        return if (appWidgetId != null && appWidgetId > 0) {
            "w_${widgetType.id}_$appWidgetId"
        } else {
            "type_${widgetType.id}"
        }
    }

    fun observeConfig(widgetType: WidgetType, appWidgetId: Int? = null): Flow<LiquidGlassConfig> {
        return context.liquidDataStore.data.map { prefs ->
            readConfigFromPrefs(prefs, widgetType, appWidgetId)
        }
    }

    suspend fun getConfig(widgetType: WidgetType, appWidgetId: Int? = null): LiquidGlassConfig {
        val prefs = context.liquidDataStore.data.first()
        return readConfigFromPrefs(prefs, widgetType, appWidgetId)
    }

    private fun readConfigFromPrefs(
        prefs: Preferences,
        widgetType: WidgetType,
        appWidgetId: Int?
    ): LiquidGlassConfig {
        val idKey = if (appWidgetId != null && appWidgetId > 0) prefix(widgetType, appWidgetId) else null
        val typeKey = prefix(widgetType, null)
        val activePrefix = if (idKey != null && prefs.contains(stringPreferencesKey("${idKey}_theme"))) {
            idKey
        } else {
            typeKey
        }
        return LiquidGlassConfig(
            themeId = prefs[stringPreferencesKey("${activePrefix}_theme")] ?: "aqua",
            thickness = prefs[floatPreferencesKey("${activePrefix}_thickness")] ?: 0.65f,
            refractionStrength = prefs[floatPreferencesKey("${activePrefix}_refraction")] ?: 0.75f,
            blur = prefs[floatPreferencesKey("${activePrefix}_blur")] ?: 0.35f,
            tintOpacity = prefs[floatPreferencesKey("${activePrefix}_tint_opacity")] ?: 0.24f,
            bubbleDensity = prefs[floatPreferencesKey("${activePrefix}_bubbles")] ?: 0.65f,
            viscosity = prefs[floatPreferencesKey("${activePrefix}_viscosity")] ?: 0.55f,
            edgeGlow = prefs[floatPreferencesKey("${activePrefix}_edge_glow")] ?: 0.78f,
            chromaticAberration = prefs[floatPreferencesKey("${activePrefix}_chromatic")] ?: 0.58f,
            shape = LiquidShape.fromId(prefs[stringPreferencesKey("${activePrefix}_shape")]),
            font = LiquidFont.fromId(prefs[stringPreferencesKey("${activePrefix}_font")]),
            fontSizeScale = prefs[floatPreferencesKey("${activePrefix}_font_scale")] ?: 1.0f,
            fontWeight = prefs[intPreferencesKey("${activePrefix}_font_weight")] ?: 600,
            backgroundMode = BackgroundMode.fromId(prefs[stringPreferencesKey("${activePrefix}_bg_mode")]),
            clockUse24Hour = prefs[booleanPreferencesKey("${activePrefix}_clock_24h")] ?: false,
            clockAnalogMode = prefs[booleanPreferencesKey("${activePrefix}_clock_analog")] ?: false,
            clockShowSeconds = prefs[booleanPreferencesKey("${activePrefix}_clock_sec")] ?: true,
            focusDurationMinutes = prefs[intPreferencesKey("${activePrefix}_focus_min")] ?: 25,
            focusNoteMode = prefs[booleanPreferencesKey("${activePrefix}_focus_note_mode")] ?: false,
            quickNoteText = prefs[stringPreferencesKey("${activePrefix}_note_text")]
                ?: "Stay hydrated & build something extraordinary.",
            dailyStepGoal = prefs[intPreferencesKey("${activePrefix}_step_goal")] ?: 10000
        )
    }

    suspend fun saveConfig(
        widgetType: WidgetType,
        config: LiquidGlassConfig,
        appWidgetId: Int? = null
    ) {
        val p = prefix(widgetType, appWidgetId)
        context.liquidDataStore.edit { prefs ->
            prefs[stringPreferencesKey("${p}_theme")] = config.themeId
            prefs[floatPreferencesKey("${p}_thickness")] = config.thickness
            prefs[floatPreferencesKey("${p}_refraction")] = config.refractionStrength
            prefs[floatPreferencesKey("${p}_blur")] = config.blur
            prefs[floatPreferencesKey("${p}_tint_opacity")] = config.tintOpacity
            prefs[floatPreferencesKey("${p}_bubbles")] = config.bubbleDensity
            prefs[floatPreferencesKey("${p}_viscosity")] = config.viscosity
            prefs[floatPreferencesKey("${p}_edge_glow")] = config.edgeGlow
            prefs[floatPreferencesKey("${p}_chromatic")] = config.chromaticAberration
            prefs[stringPreferencesKey("${p}_shape")] = config.shape.id
            prefs[stringPreferencesKey("${p}_font")] = config.font.id
            prefs[floatPreferencesKey("${p}_font_scale")] = config.fontSizeScale
            prefs[intPreferencesKey("${p}_font_weight")] = config.fontWeight
            prefs[stringPreferencesKey("${p}_bg_mode")] = config.backgroundMode.id
            prefs[booleanPreferencesKey("${p}_clock_24h")] = config.clockUse24Hour
            prefs[booleanPreferencesKey("${p}_clock_analog")] = config.clockAnalogMode
            prefs[booleanPreferencesKey("${p}_clock_sec")] = config.clockShowSeconds
            prefs[intPreferencesKey("${p}_focus_min")] = config.focusDurationMinutes
            prefs[booleanPreferencesKey("${p}_focus_note_mode")] = config.focusNoteMode
            prefs[stringPreferencesKey("${p}_note_text")] = config.quickNoteText
            prefs[intPreferencesKey("${p}_step_goal")] = config.dailyStepGoal
        }
    }

    suspend fun applyThemeToAllWidgets(preset: LiquidThemePreset) {
        WidgetType.entries.forEach { type ->
            val existing = getConfig(type)
            saveConfig(type, existing.applyTheme(preset))
        }
    }

    // Focus timer state
    suspend fun getFocusEndEpochMs(): Long =
        context.liquidDataStore.data.first()[KEY_FOCUS_END_MS] ?: 0L

    suspend fun setFocusEndEpochMs(epochMs: Long) {
        context.liquidDataStore.edit { it[KEY_FOCUS_END_MS] = epochMs }
    }

    // Weather cache
    suspend fun getCachedWeatherJson(): String? =
        context.liquidDataStore.data.first()[KEY_WEATHER_CACHE]

    suspend fun setCachedWeatherJson(json: String) {
        context.liquidDataStore.edit {
            it[KEY_WEATHER_CACHE] = json
            it[KEY_WEATHER_UPDATED_MS] = System.currentTimeMillis()
        }
    }

    // Global battery-friendly animation settings
    fun observePauseOnBatterySaver(): Flow<Boolean> =
        context.liquidDataStore.data.map { it[KEY_PAUSE_BATTERY_SAVER] ?: true }

    suspend fun setPauseOnBatterySaver(enabled: Boolean) {
        context.liquidDataStore.edit { it[KEY_PAUSE_BATTERY_SAVER] = enabled }
    }

    fun observeGyroscopeEnabled(): Flow<Boolean> =
        context.liquidDataStore.data.map { it[KEY_GYRO_ENABLED] ?: true }

    suspend fun setGyroscopeEnabled(enabled: Boolean) {
        context.liquidDataStore.edit { it[KEY_GYRO_ENABLED] = enabled }
    }

    // Touch ripple timestamp per widget for animated settle
    suspend fun triggerRipple(widgetType: WidgetType) {
        context.liquidDataStore.edit {
            it[longPreferencesKey("ripple_${widgetType.id}")] = System.currentTimeMillis()
        }
    }

    suspend fun getLastRippleMs(widgetType: WidgetType): Long =
        context.liquidDataStore.data.first()[longPreferencesKey("ripple_${widgetType.id}")] ?: 0L

    companion object {
        private val KEY_FOCUS_END_MS = longPreferencesKey("focus_end_epoch_ms")
        private val KEY_WEATHER_CACHE = stringPreferencesKey("weather_cache_json")
        private val KEY_WEATHER_UPDATED_MS = longPreferencesKey("weather_updated_ms")
        private val KEY_PAUSE_BATTERY_SAVER = booleanPreferencesKey("pause_on_battery_saver")
        private val KEY_GYRO_ENABLED = booleanPreferencesKey("gyro_enabled")
    }
}
