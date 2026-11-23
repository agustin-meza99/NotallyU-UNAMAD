package com.example.notally.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notally.R
import com.example.notally.data.model.TemaApp

// ===========================================================================
// Paleta Institucional Oficial UNAMAD (portal unamad.edu.pe):
//  - Primary: Guinda / Magenta UNAMAD (#C1034B · gradiente #D60654 -> #A80443)
//  - Secondary: Azul Marino UNAMAD (#1E2A3A · barra institucional)
//  - Tertiary: Dorado / Ámbar institucional (#9A6700 / #F5BE48)
//  - Error: Rojo Carmesí (#BA1A1A) — prioridad alta, vencimientos, borrado
//  - Éxito semántico: Verde Esmeralda (#16794A) — entregas completadas
// Contraste verificado >= 4.5:1 (WCAG 2.2 AA / ISO/IEC 25010 Usabilidad).
// ===========================================================================

internal val UnamadLightPrimary = Color(0xFFC1034B)
internal val UnamadLightOnPrimary = Color(0xFFFFFFFF)
internal val UnamadLightPrimaryContainer = Color(0xFFFFD9E2)
internal val UnamadLightOnPrimaryContainer = Color(0xFF3E0018)
internal val UnamadLightInversePrimary = Color(0xFFFFB0C8)

internal val UnamadLightSecondary = Color(0xFF1E2A3A)
internal val UnamadLightOnSecondary = Color(0xFFFFFFFF)
internal val UnamadLightSecondaryContainer = Color(0xFFD6E3F4)
internal val UnamadLightOnSecondaryContainer = Color(0xFF101C2B)

internal val UnamadLightTertiary = Color(0xFF8A5E00)
internal val UnamadLightOnTertiary = Color(0xFFFFFFFF)
internal val UnamadLightTertiaryContainer = Color(0xFFFFE09E)
internal val UnamadLightOnTertiaryContainer = Color(0xFF2B1B00)

internal val UnamadLightError = Color(0xFFBA1A1A)
internal val UnamadLightOnError = Color(0xFFFFFFFF)
internal val UnamadLightErrorContainer = Color(0xFFFFDAD6)
internal val UnamadLightOnErrorContainer = Color(0xFF410002)

internal val UnamadLightSurface = Color(0xFFFBF6F8)
internal val UnamadLightOnSurface = Color(0xFF1D1619)
internal val UnamadLightSurfaceDim = Color(0xFFE1D8DC)
internal val UnamadLightSurfaceBright = Color(0xFFFFFBFC)
internal val UnamadLightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val UnamadLightSurfaceContainerLow = Color(0xFFF6EEF1)
internal val UnamadLightSurfaceContainer = Color(0xFFF0E7EB)
internal val UnamadLightSurfaceContainerHigh = Color(0xFFEAE0E4)
internal val UnamadLightSurfaceContainerHighest = Color(0xFFE4D9DE)
internal val UnamadLightSurfaceVariant = Color(0xFFEEDDE3)
internal val UnamadLightOnSurfaceVariant = Color(0xFF514348)
internal val UnamadLightOutline = Color(0xFF837378)
internal val UnamadLightOutlineVariant = Color(0xFFD5C2C8)
internal val UnamadLightInverseSurface = Color(0xFF1E2A3A)
internal val UnamadLightInverseOnSurface = Color(0xFFF5F7FA)

// ---------------------------------------------------------------------------
// Paleta Oscura UNAMAD (Marino/Guinda Profundo + Magenta Luminoso)
// ---------------------------------------------------------------------------

internal val UnamadDarkPrimary = Color(0xFFFF7AA2)
internal val UnamadDarkOnPrimary = Color(0xFF5E0023)
internal val UnamadDarkPrimaryContainer = Color(0xFF8C0036)
internal val UnamadDarkOnPrimaryContainer = Color(0xFFFFD9E2)
internal val UnamadDarkInversePrimary = Color(0xFFC1034B)

internal val UnamadDarkSecondary = Color(0xFFB3C7E0)
internal val UnamadDarkOnSecondary = Color(0xFF1B2B3F)
internal val UnamadDarkSecondaryContainer = Color(0xFF28384D)
internal val UnamadDarkOnSecondaryContainer = Color(0xFFD6E3F4)

internal val UnamadDarkTertiary = Color(0xFFF5BE48)
internal val UnamadDarkOnTertiary = Color(0xFF442C00)
internal val UnamadDarkTertiaryContainer = Color(0xFF624100)
internal val UnamadDarkOnTertiaryContainer = Color(0xFFFFE09E)

internal val UnamadDarkError = Color(0xFFFFB4AB)
internal val UnamadDarkOnError = Color(0xFF690005)
internal val UnamadDarkErrorContainer = Color(0xFF93000A)
internal val UnamadDarkOnErrorContainer = Color(0xFFFFDAD6)

internal val UnamadDarkSurface = Color(0xFF131014)
internal val UnamadDarkOnSurface = Color(0xFFEDE0E4)
internal val UnamadDarkSurfaceDim = Color(0xFF0E0B0F)
internal val UnamadDarkSurfaceBright = Color(0xFF393237)
internal val UnamadDarkSurfaceContainerLowest = Color(0xFF0D0A0E)
internal val UnamadDarkSurfaceContainerLow = Color(0xFF1B161B)
internal val UnamadDarkSurfaceContainer = Color(0xFF211B21)
internal val UnamadDarkSurfaceContainerHigh = Color(0xFF2B242B)
internal val UnamadDarkSurfaceContainerHighest = Color(0xFF362E36)
internal val UnamadDarkSurfaceVariant = Color(0xFF514348)
internal val UnamadDarkOnSurfaceVariant = Color(0xFFD5C2C8)
internal val UnamadDarkOutline = Color(0xFF9E8C92)
internal val UnamadDarkOutlineVariant = Color(0xFF514348)
internal val UnamadDarkInverseSurface = Color(0xFF223042)
internal val UnamadDarkInverseOnSurface = Color(0xFFF5F7FA)

internal val Scrim = Color(0xFF000000)

/**
 * Tokens extendidos de identidad UNAMAD (Guinda/Magenta + Azul Marino) para
 * gradientes, acentos de prioridad e insignias académicas.
 */
@Immutable
data class UnamadExtendedColors(
    val heroStart: Color,
    val heroEnd: Color,
    val heroOnContent: Color,
    val heroBadgeBg: Color,
    val heroBadgeContent: Color,
    val goldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val successAccent: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val focalStart: Color,
    val focalEnd: Color,
    val isDark: Boolean,
)

private val UnamadExtendedLight = UnamadExtendedColors(
    heroStart = Color(0xFFD60654),
    heroEnd = Color(0xFF98023B),
    heroOnContent = Color(0xFFFFFFFF),
    heroBadgeBg = Color(0x38FFFFFF),
    heroBadgeContent = Color(0xFFFFF2F6),
    goldAccent = Color(0xFFF5BE48),
    goldContainer = Color(0xFFFFF0C4),
    onGoldContainer = Color(0xFF3D2800),
    successAccent = Color(0xFF16794A),
    successContainer = Color(0xFFD4F4DF),
    onSuccessContainer = Color(0xFF06371C),
    focalStart = Color(0xFF1E2A3A),
    focalEnd = Color(0xFF121B28),
    isDark = false,
)

private val UnamadExtendedDark = UnamadExtendedColors(
    heroStart = Color(0xFFA80443),
    heroEnd = Color(0xFF6B012A),
    heroOnContent = Color(0xFFFFF5F8),
    heroBadgeBg = Color(0x38FFFFFF),
    heroBadgeContent = Color(0xFFFFD9E2),
    goldAccent = Color(0xFFF5BE48),
    goldContainer = Color(0xFF3E2B00),
    onGoldContainer = Color(0xFFFFE09E),
    successAccent = Color(0xFF6AD996),
    successContainer = Color(0xFF0F3D22),
    onSuccessContainer = Color(0xFFC3F0D2),
    focalStart = Color(0xFF233246),
    focalEnd = Color(0xFF16202E),
    isDark = true,
)

val LocalUnamadColors = staticCompositionLocalOf { UnamadExtendedLight }

val MaterialTheme.unamad: UnamadExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalUnamadColors.current

// ---------------------------------------------------------------------------
// Esquemas Material 3
// ---------------------------------------------------------------------------

internal fun lightColorsScheme(): ColorScheme = lightColorScheme(
    primary = UnamadLightPrimary,
    onPrimary = UnamadLightOnPrimary,
    primaryContainer = UnamadLightPrimaryContainer,
    onPrimaryContainer = UnamadLightOnPrimaryContainer,
    inversePrimary = UnamadLightInversePrimary,
    secondary = UnamadLightSecondary,
    onSecondary = UnamadLightOnSecondary,
    secondaryContainer = UnamadLightSecondaryContainer,
    onSecondaryContainer = UnamadLightOnSecondaryContainer,
    tertiary = UnamadLightTertiary,
    onTertiary = UnamadLightOnTertiary,
    tertiaryContainer = UnamadLightTertiaryContainer,
    onTertiaryContainer = UnamadLightOnTertiaryContainer,
    error = UnamadLightError,
    onError = UnamadLightOnError,
    errorContainer = UnamadLightErrorContainer,
    onErrorContainer = UnamadLightOnErrorContainer,
    background = UnamadLightSurface,
    onBackground = UnamadLightOnSurface,
    surface = UnamadLightSurface,
    onSurface = UnamadLightOnSurface,
    surfaceVariant = UnamadLightSurfaceVariant,
    onSurfaceVariant = UnamadLightOnSurfaceVariant,
    surfaceTint = UnamadLightPrimary,
    inverseSurface = UnamadLightInverseSurface,
    inverseOnSurface = UnamadLightInverseOnSurface,
    outline = UnamadLightOutline,
    outlineVariant = UnamadLightOutlineVariant,
    scrim = Scrim,
    surfaceBright = UnamadLightSurfaceBright,
    surfaceDim = UnamadLightSurfaceDim,
    surfaceContainer = UnamadLightSurfaceContainer,
    surfaceContainerHigh = UnamadLightSurfaceContainerHigh,
    surfaceContainerHighest = UnamadLightSurfaceContainerHighest,
    surfaceContainerLow = UnamadLightSurfaceContainerLow,
    surfaceContainerLowest = UnamadLightSurfaceContainerLowest,
)

internal fun darkColorsScheme(): ColorScheme = darkColorScheme(
    primary = UnamadDarkPrimary,
    onPrimary = UnamadDarkOnPrimary,
    primaryContainer = UnamadDarkPrimaryContainer,
    onPrimaryContainer = UnamadDarkOnPrimaryContainer,
    inversePrimary = UnamadDarkInversePrimary,
    secondary = UnamadDarkSecondary,
    onSecondary = UnamadDarkOnSecondary,
    secondaryContainer = UnamadDarkSecondaryContainer,
    onSecondaryContainer = UnamadDarkOnSecondaryContainer,
    tertiary = UnamadDarkTertiary,
    onTertiary = UnamadDarkOnTertiary,
    tertiaryContainer = UnamadDarkTertiaryContainer,
    onTertiaryContainer = UnamadDarkOnTertiaryContainer,
    error = UnamadDarkError,
    onError = UnamadDarkOnError,
    errorContainer = UnamadDarkErrorContainer,
    onErrorContainer = UnamadDarkOnErrorContainer,
    background = UnamadDarkSurface,
    onBackground = UnamadDarkOnSurface,
    surface = UnamadDarkSurface,
    onSurface = UnamadDarkOnSurface,
    surfaceVariant = UnamadDarkSurfaceVariant,
    onSurfaceVariant = UnamadDarkOnSurfaceVariant,
    surfaceTint = UnamadDarkPrimary,
    inverseSurface = UnamadDarkInverseSurface,
    inverseOnSurface = UnamadDarkInverseOnSurface,
    outline = UnamadDarkOutline,
    outlineVariant = UnamadDarkOutlineVariant,
    scrim = Scrim,
    surfaceBright = UnamadDarkSurfaceBright,
    surfaceDim = UnamadDarkSurfaceDim,
    surfaceContainer = UnamadDarkSurfaceContainer,
    surfaceContainerHigh = UnamadDarkSurfaceContainerHigh,
    surfaceContainerHighest = UnamadDarkSurfaceContainerHighest,
    surfaceContainerLow = UnamadDarkSurfaceContainerLow,
    surfaceContainerLowest = UnamadDarkSurfaceContainerLowest,
)

// ===========================================================================
// Tipografia
// ===========================================================================

val FamiliaGeist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

val Tipografia = Typography(
    headlineSmall = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.10.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.10.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.10.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.10.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.10.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.40.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.60.sp,
    ),
)

object TipografiaNotally {
    val tituloEditor = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.20).sp,
    )

    val cuerpoEditor = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )

    val numerico = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.20.sp,
        fontFeatureSettings = "tnum",
    )
}

// ===========================================================================
// Formas
// ===========================================================================

object FormaNotally {
    val xs = RoundedCornerShape(4.dp)
    val s = RoundedCornerShape(8.dp)
    val m = RoundedCornerShape(12.dp)
    val l = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
}

val FormasNotally = Shapes(
    extraSmall = FormaNotally.xs,
    small = FormaNotally.s,
    medium = FormaNotally.m,
    large = FormaNotally.l,
    extraLarge = FormaNotally.xl,
)

// ===========================================================================
// Movimiento
// ===========================================================================

object MovimientoNotally {

    fun <T> espacialNormal(): SpringSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 380f)

    fun <T> espacialRapida(): SpringSpec<T> =
        spring(dampingRatio = 0.62f, stiffness = 780f)

    const val colorMs = 150
    const val fabMs = 160
    const val chevronMs = 180
    const val movimientoElementoMs = 240
    const val fundidoMs = 180
    const val fundidoPestanaMs = 140
    const val slidePestanaMs = 200
    const val snackbarMs = 5_000L

    val fundidoEasing = FastOutSlowInEasing
}

// ===========================================================================
// Espaciado
// ===========================================================================

object EspaciadoNotally {
    val rellenoInferiorLista = 104.dp
    val anchoMaximoContenido = 680.dp
}

val margenHorizontalPantalla: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 20.dp

// ===========================================================================
// Tema UNAMAD (Claro / Oscuro / Sistema)
// ===========================================================================

@Composable
fun TemaNotally(
    temaApp: TemaApp = TemaApp.SISTEMA,
    content: @Composable () -> Unit,
) {
    val oscuro = when (temaApp) {
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
        TemaApp.SISTEMA -> isSystemInDarkTheme()
    }
    val esquema = if (oscuro) darkColorsScheme() else lightColorsScheme()
    val extendido = if (oscuro) UnamadExtendedDark else UnamadExtendedLight

    CompositionLocalProvider(LocalUnamadColors provides extendido) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            shapes = FormasNotally,
            content = content,
        )
    }
}
