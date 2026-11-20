package com.liquidwidgets.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class WidgetType(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultSpan: String
) {
    CLOCK("clock", "Liquid Clock", "Morphing fluid numerals & rising minute fill", "4×2 / 2×2"),
    BATTERY("battery", "Liquid Battery", "Live liquid tank with waves & charging wattage", "2×2 / 4×2"),
    SCREEN_TIME("screentime", "Screen Time", "Liquid ring & top 3 app droplets on-device", "4×2"),
    WEATHER("weather", "Liquid Weather", "Rain droplets, solar glow & hourly bubbles", "4×2 / 4×4"),
    CALENDAR("calendar", "Calendar & Agenda", "Next 3 events in floating glass pills", "4×2"),
    MUSIC("music", "Music Player", "Refracted album art & live liquid audio waves", "4×2"),
    STEPS("steps", "Steps & Health", "Water-pouring step goal ring & bobbing droplet", "2×2"),
    SYSTEM("system", "System Monitor", "CPU, RAM & Storage sloshing liquid gauges", "2×2"),
    FOCUS_NOTES("focus", "Focus Timer & Notes", "Draining Pomodoro droplet & floating ink note", "2×2"),
    QUICK_PILL("quickpill", "Quick Glass Pill", "Wi-Fi, BT, Torch, DND bubbles + app search", "4×1");

    companion object {
        fun fromId(id: String?): WidgetType = entries.firstOrNull { it.id == id } ?: CLOCK
    }
}

enum class LiquidShape(val id: String, val label: String, val shaderIndex: Float) {
    SQUIRCLE("squircle", "Squircle", 0f),
    BLOB("blob", "Organic Blob", 1f),
    PILL("pill", "Pill", 2f),
    SHARP_GLASS("sharp", "Sharp Glass", 3f),
    FREEFORM("freeform", "Freeform", 4f);

    companion object {
        fun fromId(id: String?): LiquidShape = entries.firstOrNull { it.id == id } ?: SQUIRCLE
    }
}

enum class LiquidFont(
    val id: String,
    val label: String,
    val family: FontFamily,
    val defaultWeight: Int
) {
    FLUID_SANS("fluid_sans", "Fluid Sans", FontFamily.SansSerif, 600),
    LIQUID_SERIF("liquid_serif", "Liquid Serif", FontFamily.Serif, 500),
    DROPLET_MONO("droplet_mono", "Droplet Mono", FontFamily.Monospace, 500),
    ORGANIC_ROUNDED("organic_rounded", "Organic Rounded", FontFamily.SansSerif, 700),
    MERCURY_CONDENSED("mercury_condensed", "Mercury Thin", FontFamily.SansSerif, 300),
    GLASS_DISPLAY("glass_display", "Glass Display", FontFamily.Serif, 700);

    fun toFontWeight(overrideWeight: Int): FontWeight = when {
        overrideWeight <= 300 -> FontWeight.Light
        overrideWeight <= 400 -> FontWeight.Normal
        overrideWeight <= 500 -> FontWeight.Medium
        overrideWeight <= 600 -> FontWeight.SemiBold
        else -> FontWeight.Bold
    }

    companion object {
        fun fromId(id: String?): LiquidFont = entries.firstOrNull { it.id == id } ?: FLUID_SANS
    }
}

enum class BackgroundMode(val id: String, val label: String) {
    SAMPLE_WALLPAPER("wallpaper", "Sample Wallpaper (Live)"),
    SOLID_TINT("solid", "Solid Tint"),
    TRANSPARENT("transparent", "Clear Glass");

    companion object {
        fun fromId(id: String?): BackgroundMode = entries.firstOrNull { it.id == id } ?: SAMPLE_WALLPAPER
    }
}

data class LiquidThemePreset(
    val id: String,
    val name: String,
    val tagline: String,
    val tintArgb: Long,
    val accentArgb: Long,
    val textArgb: Long,
    val defaultThickness: Float = 0.65f,
    val defaultRefraction: Float = 0.72f,
    val defaultBlur: Float = 0.35f,
    val defaultTintOpacity: Float = 0.24f,
    val defaultBubbleDensity: Float = 0.65f,
    val defaultViscosity: Float = 0.55f,
    val defaultEdgeGlow: Float = 0.75f,
    val defaultChromaticAberration: Float = 0.55f
) {
    val tintColor: Color get() = Color(tintArgb)
    val accentColor: Color get() = Color(accentArgb)
    val textColor: Color get() = Color(textArgb)
}

object LiquidThemes {
    val ALL: List<LiquidThemePreset> = listOf(
        LiquidThemePreset(
            id = "aqua",
            name = "Aqua",
            tagline = "Crystal coastal water refraction",
            tintArgb = 0xFF1EC8E8,
            accentArgb = 0xFF8DF4FF,
            textArgb = 0xFFF4FDFF,
            defaultRefraction = 0.78f,
            defaultChromaticAberration = 0.58f
        ),
        LiquidThemePreset(
            id = "mercury",
            name = "Mercury",
            tagline = "Liquid chrome with high-gloss speculars",
            tintArgb = 0xFFB8C6D6,
            accentArgb = 0xFFFFFFFF,
            textArgb = 0xFFFFFFFF,
            defaultThickness = 0.85f,
            defaultRefraction = 0.88f,
            defaultTintOpacity = 0.34f,
            defaultViscosity = 0.35f,
            defaultEdgeGlow = 0.95f
        ),
        LiquidThemePreset(
            id = "honey",
            name = "Honey",
            tagline = "Warm viscous amber syrup",
            tintArgb = 0xFFE59A1A,
            accentArgb = 0xFFFFDF73,
            textArgb = 0xFFFFFAF0,
            defaultThickness = 0.82f,
            defaultTintOpacity = 0.32f,
            defaultViscosity = 0.25f,
            defaultBubbleDensity = 0.45f
        ),
        LiquidThemePreset(
            id = "neon_plasma",
            name = "Neon Plasma",
            tagline = "Electric cyan-magenta chromatic dispersion",
            tintArgb = 0xFF8A2BE2,
            accentArgb = 0xFF00F5FF,
            textArgb = 0xFFFFFFFF,
            defaultRefraction = 0.90f,
            defaultEdgeGlow = 0.95f,
            defaultChromaticAberration = 0.95f,
            defaultViscosity = 0.85f
        ),
        LiquidThemePreset(
            id = "deep_ocean",
            name = "Deep Ocean",
            tagline = "Abyssal cobalt glass with bioluminescent rim",
            tintArgb = 0xFF0A325C,
            accentArgb = 0xFF3BD6FF,
            textArgb = 0xFFEAF7FF,
            defaultThickness = 0.80f,
            defaultTintOpacity = 0.42f,
            defaultBubbleDensity = 0.80f
        ),
        LiquidThemePreset(
            id = "frosted_milk",
            name = "Frosted Milk",
            tagline = "Soft milky optical quartz with warm edge",
            tintArgb = 0xFFEAF2F8,
            accentArgb = 0xFFFFFFFF,
            textArgb = 0xFF102030,
            defaultBlur = 0.72f,
            defaultTintOpacity = 0.48f,
            defaultRefraction = 0.55f,
            defaultChromaticAberration = 0.30f
        ),
        LiquidThemePreset(
            id = "obsidian",
            name = "Obsidian",
            tagline = "Smoked volcanic glass with razor highlights",
            tintArgb = 0xFF10151E,
            accentArgb = 0xFF6AA8FF,
            textArgb = 0xFFF5F8FF,
            defaultThickness = 0.90f,
            defaultTintOpacity = 0.54f,
            defaultEdgeGlow = 0.85f
        ),
        LiquidThemePreset(
            id = "sunset_syrup",
            name = "Sunset Syrup",
            tagline = "Coral-peach glass glowing at golden hour",
            tintArgb = 0xFFE85A4F,
            accentArgb = 0xFFFFC178,
            textArgb = 0xFFFFF8F2,
            defaultTintOpacity = 0.30f,
            defaultRefraction = 0.76f,
            defaultChromaticAberration = 0.68f
        ),
        LiquidThemePreset(
            id = "mint",
            name = "Mint",
            tagline = "Glacial spearmint dewdrop glass",
            tintArgb = 0xFF1FD1A5,
            accentArgb = 0xFFA8FFE6,
            textArgb = 0xFFF2FFF9,
            defaultTintOpacity = 0.26f,
            defaultBubbleDensity = 0.75f
        ),
        LiquidThemePreset(
            id = "lavender_mist",
            name = "Lavender Mist",
            tagline = "Dreamy amethyst optical lens",
            tintArgb = 0xFF9B7BFF,
            accentArgb = 0xFFE0C8FF,
            textArgb = 0xFFFAF6FF,
            defaultBlur = 0.48f,
            defaultTintOpacity = 0.28f,
            defaultChromaticAberration = 0.62f
        ),
        LiquidThemePreset(
            id = "blood_moon",
            name = "Blood Moon",
            tagline = "Crimson ruby liquid with volcanic sheen",
            tintArgb = 0xFF9E1128,
            accentArgb = 0xFFFF5E73,
            textArgb = 0xFFFFF2F4,
            defaultThickness = 0.84f,
            defaultTintOpacity = 0.42f,
            defaultViscosity = 0.42f
        ),
        LiquidThemePreset(
            id = "clear_water",
            name = "Clear Water",
            tagline = "Pure uncolored water droplet magnification",
            tintArgb = 0xFFD8F6FF,
            accentArgb = 0xFFFFFFFF,
            textArgb = 0xFFFFFFFF,
            defaultThickness = 0.72f,
            defaultRefraction = 0.92f,
            defaultBlur = 0.12f,
            defaultTintOpacity = 0.09f,
            defaultEdgeGlow = 0.92f,
            defaultChromaticAberration = 0.72f
        )
    )

    fun byId(id: String?): LiquidThemePreset = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

data class LiquidGlassConfig(
    val themeId: String = "aqua",
    val thickness: Float = 0.65f,
    val refractionStrength: Float = 0.75f,
    val blur: Float = 0.35f,
    val tintOpacity: Float = 0.24f,
    val bubbleDensity: Float = 0.65f,
    val viscosity: Float = 0.55f,
    val edgeGlow: Float = 0.78f,
    val chromaticAberration: Float = 0.58f,
    val shape: LiquidShape = LiquidShape.SQUIRCLE,
    val font: LiquidFont = LiquidFont.FLUID_SANS,
    val fontSizeScale: Float = 1.0f,
    val fontWeight: Int = 600,
    val backgroundMode: BackgroundMode = BackgroundMode.SAMPLE_WALLPAPER,
    // Widget-specific options
    val clockUse24Hour: Boolean = false,
    val clockAnalogMode: Boolean = false,
    val clockShowSeconds: Boolean = true,
    val focusDurationMinutes: Int = 25,
    val focusNoteMode: Boolean = false,
    val quickNoteText: String = "Tap to edit floating glass note",
    val dailyStepGoal: Int = 10000
) {
    val theme: LiquidThemePreset get() = LiquidThemes.byId(themeId)

    fun applyTheme(preset: LiquidThemePreset): LiquidGlassConfig = copy(
        themeId = preset.id,
        thickness = preset.defaultThickness,
        refractionStrength = preset.defaultRefraction,
        blur = preset.defaultBlur,
        tintOpacity = preset.defaultTintOpacity,
        bubbleDensity = preset.defaultBubbleDensity,
        viscosity = preset.defaultViscosity,
        edgeGlow = preset.defaultEdgeGlow,
        chromaticAberration = preset.defaultChromaticAberration
    )
}
