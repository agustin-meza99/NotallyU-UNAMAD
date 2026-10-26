package com.example.notally.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notally.R

// ===========================================================================
// Colores
// ===========================================================================

// ---------------------------------------------------------------------------
// Paleta clara (§2.2 del DESIGN.md). Es la única paleta de la app.
//
// Generada con @material/material-color-utilities 0.3.0, esquema TonalSpot
// claro a partir del seed #2B5773; no hay hex elegidos a mano. Cada constante
// es un tono de la paleta tonal del hue correspondiente, con el mapeo que
// documenta §2.2.
// ---------------------------------------------------------------------------

internal val LightPrimary = Color(0xFF246488)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFC8E6FF)
internal val LightOnPrimaryContainer = Color(0xFF004C6E)
internal val LightInversePrimary = Color(0xFF94CDF7)
internal val LightSecondary = Color(0xFF4F606E)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFD3E5F5)
internal val LightOnSecondaryContainer = Color(0xFF384956)
internal val LightTertiary = Color(0xFF64597C)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFE9DDFF)
internal val LightOnTertiaryContainer = Color(0xFF4B4163)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF93000A)

/**
 * Superficie base del tema claro.
 *
 * La fuente única del valor es `res/values/colors.xml`: `themes.xml` la usa
 * para el `windowBackground` y aquí se lee de ahí para el esquema M3.
 */
internal val LightSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = colorResource(R.color.notally_surface)

internal val LightOnSurface = Color(0xFF181C20)
internal val LightSurfaceDim = Color(0xFFD7DADF)
internal val LightSurfaceBright = Color(0xFFF6F9FE)
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFF1F4F9)
internal val LightSurfaceContainer = Color(0xFFEBEEF3)
internal val LightSurfaceContainerHigh = Color(0xFFE5E8ED)
internal val LightSurfaceContainerHighest = Color(0xFFDFE3E8)
internal val LightSurfaceVariant = Color(0xFFDDE3EA)
internal val LightOnSurfaceVariant = Color(0xFF41474D)
internal val LightOutline = Color(0xFF71787E)
internal val LightOutlineVariant = Color(0xFFC1C7CE)
internal val LightInverseSurface = Color(0xFF2D3135)
internal val LightInverseOnSurface = Color(0xFFEEF1F6)

internal val Scrim = Color(0xFF000000)

// ---------------------------------------------------------------------------
// Esquemas Material 3
// ---------------------------------------------------------------------------

@Composable
internal fun lightColorsScheme(): ColorScheme {
    val superficie = LightSurface
    return remember(superficie) { lightScheme(superficie) }
}

private fun lightScheme(superficie: Color): ColorScheme = lightColorScheme(
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
    background = superficie,
    onBackground = LightOnSurface,
    surface = superficie,
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

// ===========================================================================
// Tipografia
// ===========================================================================

/**
 * Geist (Vercel, OFL 1.1) en tres pesos. Sustituye a la sans del sistema; el
 * serif quedó retirado el 2026-10-26. Licencia en `res/raw/geist_ofl.txt`.
 */
val FamiliaGeist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/**
 * Escala tipográfica del DESIGN.md §3.2.
 *
 * Sin serif (retirado el 2026-10-26 junto con los títulos de pantalla). Los
 * slots `display*` y `headlineLarge/Medium` no se declaran: nadie los usa y
 * Material 3 aporta sus defaults.
 */
val Tipografia = Typography(
    headlineSmall = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FamiliaGeist,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
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
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.60.sp,
    ),
)

/**
 * Estilos extra fuera de [Typography] (§3.2).
 *
 * `numeric` activa cifras tabulares (`tnum`) para que los conteos no bailen al
 * actualizarse.
 */
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
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.20.sp,
        fontFeatureSettings = "tnum",
    )
}

// ===========================================================================
// Formas
// ===========================================================================

/** Acceso directo a los tokens de forma del DESIGN.md §4.2. */
object FormaNotally {
    val xs = RoundedCornerShape(4.dp)
    val s = RoundedCornerShape(8.dp)
    val m = RoundedCornerShape(12.dp)
    val l = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(28.dp)
}

/**
 * Slots de Material 3, derivados de [FormaNotally]: extraSmall = 4,
 * small = 8, medium = 12, large = 16, extraLarge = 28.
 */
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

/**
 * Motion del DESIGN.md §5.
 *
 * En material3 1.4.0 `MotionScheme` es `internal`, así que los springs
 * expresivos se copian como constantes de la app (valores verificados en el
 * artefacto). No se fuerza duración: si el sistema desactiva animaciones,
 * Compose respeta `MotionDurationScale`.
 */
object MovimientoNotally {

    // Springs espaciales: tamaño, posición, expansión.
    fun <T> espacialNormal(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> espacialRapida(): SpringSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    // Duraciones puntuales (§5, excepciones permitidas).
    const val colorMs = 120
    const val fabMs = 150
    const val chevronMs = 180
    const val movimientoElementoMs = 220
    const val fundidoMs = 180
    const val fundidoPestanaMs = 120
    const val slidePestanaMs = 180
    const val snackbarMs = 5_000L

    val fundidoEasing = FastOutSlowInEasing
}

// ===========================================================================
// Espaciado
// ===========================================================================

/** Espaciado del DESIGN.md §4.1 (múltiplos de 4). */
object EspaciadoNotally {

    /** Padding inferior de listas que comparten espacio con el cluster (§4.1, §6). */
    val rellenoInferiorLista = 96.dp

    /** Ancho máximo de lectura. */
    val anchoMaximoContenido = 680.dp
}

/**
 * Margen lateral de pantalla: 20 dp; 24 dp en ventanas de 600 dp o más.
 * Se calcula con la configuración local para no fijar un valor único.
 */
val margenHorizontalPantalla: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 20.dp

// ===========================================================================
// Tema
// ===========================================================================

/**
 * Tema de Notally.
 *
 * Un solo tema claro y fijo: siempre [lightColorsScheme]. No existe variante
 * oscura, dinámica ni ajuste que altere los roles M3.
 */
@Composable
fun TemaNotally(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorsScheme(),
        typography = Tipografia,
        shapes = FormasNotally,
        content = content,
    )
}
