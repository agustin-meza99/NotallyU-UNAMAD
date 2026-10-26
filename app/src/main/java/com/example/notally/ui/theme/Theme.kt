package com.example.notally.ui.theme

import androidx.annotation.StringRes
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.example.notally.data.ColorsCurso
import kotlin.math.pow

// ===========================================================================
// Colores
// ===========================================================================

// ---------------------------------------------------------------------------
// Helpers de distancia de color (solo para resolver acentos de curso).
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
// Paleta clara (§2.2 del DESIGN.md). Es la única paleta de la app.
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

// ---------------------------------------------------------------------------
// Acentos de curso (§2.5). Seis swatches fijos, con una sola variante clara.
// El argb almacenado por el usuario es el hex claro del swatch.
// ---------------------------------------------------------------------------

/**
 * Un swatch de curso.
 *
 * [argb] es el valor canónico que se guarda en `Curso.colorArgb` (hex claro).
 * [light] identifica el curso sobre superficie; [lightContainer] se usa en la
 * hoja de edición y en la selección de swatch.
 */
@Immutable
data class SwatchCurso(
    val argb: Long,
    @param:StringRes val nameRes: Int,
    val light: Color,
    val lightContainer: Color,
)

/** Los seis acentos, en el orden de la tabla del documento. */
val SwatchesCursos: List<SwatchCurso> = listOf(
    SwatchCurso(
        argb = ColorsCurso.INDIGO,
        nameRes = R.string.course_color_indigo,
        light = Color(ColorsCurso.INDIGO),
        lightContainer = Color(0xFFE9ECF5),
    ),
    SwatchCurso(
        argb = ColorsCurso.FOREST,
        nameRes = R.string.course_color_forest,
        light = Color(ColorsCurso.FOREST),
        lightContainer = Color(0xFFE6EFE8),
    ),
    SwatchCurso(
        argb = ColorsCurso.TERRACOTTA,
        nameRes = R.string.course_color_terracotta,
        light = Color(ColorsCurso.TERRACOTTA),
        lightContainer = Color(0xFFF4E8E2),
    ),
    SwatchCurso(
        argb = ColorsCurso.LIME,
        nameRes = R.string.course_color_lime,
        light = Color(ColorsCurso.LIME),
        lightContainer = Color(0xFFF3F5E0),
    ),
    SwatchCurso(
        argb = ColorsCurso.LAVENDER,
        nameRes = R.string.course_color_lavender,
        light = Color(ColorsCurso.LAVENDER),
        lightContainer = Color(0xFFF3EDF5),
    ),
    SwatchCurso(
        argb = ColorsCurso.PETROL,
        nameRes = R.string.course_color_petrol,
        light = Color(ColorsCurso.PETROL),
        lightContainer = Color(0xFFE3EEEE),
    ),
)

/**
 * Resuelve el acento de un curso contra los seis swatches claros.
 *
 * Los valores guardados con anterioridad y los de bases anteriores pueden no
 * coincidir con los seis swatches; en ese caso se elige el swatch más cercano en RGB para que la
 * identidad de color del documento se mantenga (nunca se muestra un morado
 * suelto fuera de la paleta).
 */
@Immutable
class PaletteCursos internal constructor(
    val swatches: List<SwatchCurso> = SwatchesCursos,
) {
    private fun resolve(argb: Long): SwatchCurso {
        val normalized = argb and 0xFFFFFFFFL
        swatches.firstOrNull { it.argb == normalized }?.let { return it }
        return swatches.minBy { colorDistance(normalized, it.argb) }
    }

    fun colorCurso(argb: Long): Color = resolve(argb).light

    fun contenedorCurso(argb: Long): Color = resolve(argb).lightContainer

    fun swatchCurso(argb: Long): SwatchCurso = resolve(argb)

    companion object {
        val Single = PaletteCursos()
    }
}

/** Paleta de curso del tema claro. */
val LocalPaletteCursos = staticCompositionLocalOf { PaletteCursos.Single }

/** Color de acento de curso resuelto para el tema claro. */
@Composable
@ReadOnlyComposable
fun colorCurso(argb: Long): Color = LocalPaletteCursos.current.colorCurso(argb)

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
 * Un solo tema claro y fijo: siempre [lightColorsScheme] con la paleta de
 * cursos clara. No existe variante oscura, dinámica ni ajuste que altere los
 * roles M3.
 */
@Composable
fun TemaNotally(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPaletteCursos provides PaletteCursos.Single) {
        MaterialTheme(
            colorScheme = lightColorsScheme(),
            typography = Tipografia,
            shapes = FormasNotally,
            content = content,
        )
    }
}
