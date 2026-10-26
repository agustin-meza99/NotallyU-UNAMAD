package com.example.notally.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.notally.R
import com.example.notally.data.SubjectColors
import kotlin.math.pow

// ---------------------------------------------------------------------------
// Helpers de distancia de color (solo para resolver acentos de materia).
// ---------------------------------------------------------------------------

private fun rgb(argb: Long): Triple<Int, Int, Int> = Triple(
    ((argb shr 16) and 0xFF).toInt(),
    ((argb shr 8) and 0xFF).toInt(),
    (argb and 0xFF).toInt(),
)

private fun colorDistance(a: Long, b: Long): Double {
    val (ar, ag, ab) = rgb(a)
    val (br, bg, bb) = rgb(b)
    // Distancia ponderada (aprox. percepción): el ojo es más sensible al verde.
    return (2 * (ar - br).toDouble().pow(2) +
        4 * (ag - bg).toDouble().pow(2) +
        3 * (ab - bb).toDouble().pow(2))
}

// ---------------------------------------------------------------------------
// Paleta clara (§2.2 del DESIGN.md). Modo claro propio, no inversión del oscuro.
// ---------------------------------------------------------------------------

internal val LightPrimary = Color(0xFF262320)
internal val LightOnPrimary = Color(0xFFFCFAF6)
internal val LightPrimaryContainer = Color(0xFFE7E1D6)
internal val LightOnPrimaryContainer = Color(0xFF262320)
internal val LightInversePrimary = Color(0xFFD6CEC0)
internal val LightSecondary = Color(0xFF5E594F)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFEAE4D9)
internal val LightOnSecondaryContainer = Color(0xFF38342D)
internal val LightTertiary = Color(0xFF2B5773)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFD6E5EF)
internal val LightOnTertiaryContainer = Color(0xFF103246)
internal val LightError = Color(0xFFA33A2C)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFF7DDD8)
internal val LightOnErrorContainer = Color(0xFF4A130B)
internal val LightSurface = Color(0xFFFBFAF7)
internal val LightOnSurface = Color(0xFF1F1C18)
internal val LightSurfaceDim = Color(0xFFE6E1D8)
internal val LightSurfaceBright = Color(0xFFFFFDF9)
internal val LightSurfaceContainerLowest = Color(0xFFFDFBF8)
internal val LightSurfaceContainerLow = Color(0xFFF5F2ED)
internal val LightSurfaceContainer = Color(0xFFF0ECE5)
internal val LightSurfaceContainerHigh = Color(0xFFE9E4DC)
internal val LightSurfaceContainerHighest = Color(0xFFE2DCD2)
internal val LightSurfaceVariant = Color(0xFFE8E3DA)
internal val LightOnSurfaceVariant = Color(0xFF5B554C)
internal val LightOutline = Color(0xFF8A8275)
internal val LightOutlineVariant = Color(0xFFD7D0C4)
internal val LightInverseSurface = Color(0xFF2E2A25)
internal val LightInverseOnSurface = Color(0xFFF6F2EA)

// ---------------------------------------------------------------------------
// Paleta oscura (§2.3). Tinta cálida, no negro puro.
// ---------------------------------------------------------------------------

internal val DarkPrimary = Color(0xFFE4DED3)
internal val DarkOnPrimary = Color(0xFF23201C)
internal val DarkPrimaryContainer = Color(0xFF3A342B)
internal val DarkOnPrimaryContainer = Color(0xFFF0EAE0)
internal val DarkInversePrimary = Color(0xFF4A4339)
internal val DarkSecondary = Color(0xFFC5BEB2)
internal val DarkOnSecondary = Color(0xFF2A2620)
internal val DarkSecondaryContainer = Color(0xFF37322A)
internal val DarkOnSecondaryContainer = Color(0xFFE4DDD2)
internal val DarkTertiary = Color(0xFFA6CBE3)
internal val DarkOnTertiary = Color(0xFF0D2D40)
internal val DarkTertiaryContainer = Color(0xFF1D4157)
internal val DarkOnTertiaryContainer = Color(0xFFCDE6F5)
internal val DarkError = Color(0xFFF2B8AD)
internal val DarkOnError = Color(0xFF5C160C)
internal val DarkErrorContainer = Color(0xFF6E2418)
internal val DarkOnErrorContainer = Color(0xFFF9DCD6)
internal val DarkSurface = Color(0xFF151310)
internal val DarkOnSurface = Color(0xFFECE7DE)
internal val DarkSurfaceDim = Color(0xFF110F0D)
internal val DarkSurfaceBright = Color(0xFF3A342C)
internal val DarkSurfaceContainerLowest = Color(0xFF100E0C)
internal val DarkSurfaceContainerLow = Color(0xFF1C1916)
internal val DarkSurfaceContainer = Color(0xFF211E1A)
internal val DarkSurfaceContainerHigh = Color(0xFF29251F)
internal val DarkSurfaceContainerHighest = Color(0xFF322D26)
internal val DarkSurfaceVariant = Color(0xFF3A352E)
internal val DarkOnSurfaceVariant = Color(0xFFB3AB9F)
internal val DarkOutline = Color(0xFF8B8377)
internal val DarkOutlineVariant = Color(0xFF4A443C)
internal val DarkInverseSurface = Color(0xFFECE7DE)
internal val DarkInverseOnSurface = Color(0xFF322E28)

internal val Scrim = Color(0xFF000000)

// ---------------------------------------------------------------------------
// Esquemas Material 3
// ---------------------------------------------------------------------------

internal val NotallyLightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = LightInversePrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Scrim,
    surfaceBright = LightSurfaceBright,
    surfaceDim = LightSurfaceDim,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerLowest = LightSurfaceContainerLowest,
)

internal val NotallyDarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = DarkInversePrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Scrim,
    surfaceBright = DarkSurfaceBright,
    surfaceDim = DarkSurfaceDim,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
)

// ---------------------------------------------------------------------------
// Acentos de materia (§2.5). Seis swatches fijos, con variante clara y oscura.
// El argb almacenado por el usuario es el hex claro del swatch.
// ---------------------------------------------------------------------------

/**
 * Un swatch de materia.
 *
 * [argb] es el valor canónico que se guarda en `Subject.colorArgb` (hex claro).
 * [light] / [dark] son los colores de identificación sobre superficie; los
 * contenedores se usan en la hoja de edición y en selección de swatch.
 */
@Immutable
data class SubjectSwatch(
    val argb: Long,
    @param:StringRes val nameRes: Int,
    val light: Color,
    val dark: Color,
    val lightContainer: Color,
    val darkContainer: Color,
) {
    fun accent(isDark: Boolean): Color = if (isDark) dark else light
    fun container(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer

    /** Texto/icono sobre el contenedor: el acento en su variante de tema. */
    fun onContainer(isDark: Boolean): Color = accent(isDark)
}

/** Los seis acentos, en el orden de la tabla del documento. */
val NotallySubjectSwatches: List<SubjectSwatch> = listOf(
    SubjectSwatch(
        argb = SubjectColors.INDIGO,
        nameRes = R.string.subject_color_indigo,
        light = Color(SubjectColors.INDIGO),
        dark = Color(0xFFAFC3EE),
        lightContainer = Color(0xFFE9ECF5),
        darkContainer = Color(0xFF222B3F),
    ),
    SubjectSwatch(
        argb = SubjectColors.FOREST,
        nameRes = R.string.subject_color_forest,
        light = Color(SubjectColors.FOREST),
        dark = Color(0xFF9FC9B2),
        lightContainer = Color(0xFFE6EFE8),
        darkContainer = Color(0xFF1D2C24),
    ),
    SubjectSwatch(
        argb = SubjectColors.TERRACOTTA,
        nameRes = R.string.subject_color_terracotta,
        light = Color(SubjectColors.TERRACOTTA),
        dark = Color(0xFFE7AE97),
        lightContainer = Color(0xFFF4E8E2),
        darkContainer = Color(0xFF33231C),
    ),
    SubjectSwatch(
        argb = SubjectColors.LIME,
        nameRes = R.string.subject_color_lime,
        light = Color(SubjectColors.LIME),
        dark = Color(0xFFD8E5A0),
        lightContainer = Color(0xFFF3F5E0),
        darkContainer = Color(0xFF2A2B16),
    ),
    SubjectSwatch(
        argb = SubjectColors.LAVENDER,
        nameRes = R.string.subject_color_lavender,
        light = Color(SubjectColors.LAVENDER),
        dark = Color(0xFFCAA8B8),
        lightContainer = Color(0xFFF3EDF5),
        darkContainer = Color(0xFF2B2233),
    ),
    SubjectSwatch(
        argb = SubjectColors.PETROL,
        nameRes = R.string.subject_color_petrol,
        light = Color(SubjectColors.PETROL),
        dark = Color(0xFF96CDD6),
        lightContainer = Color(0xFFE3EEEE),
        darkContainer = Color(0xFF182B2E),
    ),
)

/**
 * Resuelve el acento de una materia para el tema actual.
 *
 * Los valores del seed y de bases anteriores pueden no coincidir con los seis
 * swatches; en ese caso se elige el swatch más cercano en RGB para que la
 * identidad de color del documento se mantenga (nunca se muestra un morado
 * suelto fuera de la paleta).
 */
@Immutable
class SubjectPalette internal constructor(
    val isDark: Boolean,
    val swatches: List<SubjectSwatch> = NotallySubjectSwatches,
) {
    private fun resolve(argb: Long): SubjectSwatch {
        val normalized = argb and 0xFFFFFFFFL
        swatches.firstOrNull { it.argb == normalized }?.let { return it }
        // Algunos seeds guardan el hex oscuro del swatch; también se acepta.
        swatches.firstOrNull { it.dark.toArgb().toLong() and 0xFFFFFFFFL == normalized }
            ?.let { return it }
        return swatches.minBy { colorDistance(normalized, it.argb) }
    }

    fun subjectColor(argb: Long): Color = resolve(argb).accent(isDark)

    fun subjectContainer(argb: Long): Color = resolve(argb).container(isDark)

    fun subjectOnContainer(argb: Long): Color = resolve(argb).onContainer(isDark)

    fun subjectNameRes(argb: Long): Int = resolve(argb).nameRes

    fun subjectSwatch(argb: Long): SubjectSwatch = resolve(argb)

    companion object {
        val Light = SubjectPalette(isDark = false)
        val Dark = SubjectPalette(isDark = true)
    }
}

/** Paleta de materia del tema activo. */
val LocalSubjectPalette = staticCompositionLocalOf { SubjectPalette.Light }

/** Color de acento de materia resuelto para el tema actual. */
@androidx.compose.runtime.Composable
@androidx.compose.runtime.ReadOnlyComposable
fun subjectColor(argb: Long): Color = LocalSubjectPalette.current.subjectColor(argb)
