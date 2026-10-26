package com.example.notally.ui.theme

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notally.R
import com.example.notally.data.ColoresCurso
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

private fun distanciaColor(a: Long, b: Long): Double {
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

internal val EsquemaColoresClaro: ColorScheme = lightColorScheme(
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

internal val EsquemaColoresOscuro: ColorScheme = darkColorScheme(
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
// Acentos de curso (§2.5). Seis swatches fijos, con variante clara y oscura.
// El argb almacenado por el usuario es el hex claro del swatch.
// ---------------------------------------------------------------------------

/**
 * Un swatch de curso.
 *
 * [argb] es el valor canónico que se guarda en `Curso.colorArgb` (hex claro).
 * [light] / [dark] son los colores de identificación sobre superficie; los
 * contenedores se usan en la hoja de edición y en selección de swatch.
 */
@Immutable
data class MuestraCurso(
    val argb: Long,
    @param:StringRes val recursoNombre: Int,
    val claro: Color,
    val oscuro: Color,
    val contenedorClaro: Color,
    val contenedorOscuro: Color,
) {
    fun acento(esOscuro: Boolean): Color = if (esOscuro) oscuro else claro
    fun contenedor(esOscuro: Boolean): Color = if (esOscuro) contenedorOscuro else contenedorClaro

    /** Texto/icono sobre el contenedor: el acento en su variante de tema. */
    fun sobreContenedor(esOscuro: Boolean): Color = acento(esOscuro)
}

/** Los seis acentos, en el orden de la tabla del documento. */
val MuestrasCursos: List<MuestraCurso> = listOf(
    MuestraCurso(
        argb = ColoresCurso.INDIGO,
        recursoNombre = R.string.subject_color_indigo,
        claro = Color(ColoresCurso.INDIGO),
        oscuro = Color(0xFFAFC3EE),
        contenedorClaro = Color(0xFFE9ECF5),
        contenedorOscuro = Color(0xFF222B3F),
    ),
    MuestraCurso(
        argb = ColoresCurso.FOREST,
        recursoNombre = R.string.subject_color_forest,
        claro = Color(ColoresCurso.FOREST),
        oscuro = Color(0xFF9FC9B2),
        contenedorClaro = Color(0xFFE6EFE8),
        contenedorOscuro = Color(0xFF1D2C24),
    ),
    MuestraCurso(
        argb = ColoresCurso.TERRACOTTA,
        recursoNombre = R.string.subject_color_terracotta,
        claro = Color(ColoresCurso.TERRACOTTA),
        oscuro = Color(0xFFE7AE97),
        contenedorClaro = Color(0xFFF4E8E2),
        contenedorOscuro = Color(0xFF33231C),
    ),
    MuestraCurso(
        argb = ColoresCurso.LIME,
        recursoNombre = R.string.subject_color_lime,
        claro = Color(ColoresCurso.LIME),
        oscuro = Color(0xFFD8E5A0),
        contenedorClaro = Color(0xFFF3F5E0),
        contenedorOscuro = Color(0xFF2A2B16),
    ),
    MuestraCurso(
        argb = ColoresCurso.LAVENDER,
        recursoNombre = R.string.subject_color_lavender,
        claro = Color(ColoresCurso.LAVENDER),
        oscuro = Color(0xFFCAA8B8),
        contenedorClaro = Color(0xFFF3EDF5),
        contenedorOscuro = Color(0xFF2B2233),
    ),
    MuestraCurso(
        argb = ColoresCurso.PETROL,
        recursoNombre = R.string.subject_color_petrol,
        claro = Color(ColoresCurso.PETROL),
        oscuro = Color(0xFF96CDD6),
        contenedorClaro = Color(0xFFE3EEEE),
        contenedorOscuro = Color(0xFF182B2E),
    ),
)

/**
 * Resuelve el acento de un curso para el tema actual.
 *
 * Los valores guardados con anterioridad y los de bases anteriores pueden no
 * coincidir con los seis swatches; en ese caso se elige el swatch más cercano en RGB para que la
 * identidad de color del documento se mantenga (nunca se muestra un morado
 * suelto fuera de la paleta).
 */
@Immutable
class PaletaCursos internal constructor(
    val esOscuro: Boolean,
    val muestras: List<MuestraCurso> = MuestrasCursos,
) {
    private fun resolver(argb: Long): MuestraCurso {
        val normalized = argb and 0xFFFFFFFFL
        muestras.firstOrNull { it.argb == normalized }?.let { return it }
        // Algunos datos antiguos guardan el hex oscuro del swatch; también se acepta.
        muestras.firstOrNull { it.oscuro.toArgb().toLong() and 0xFFFFFFFFL == normalized }
            ?.let { return it }
        return muestras.minBy { distanciaColor(normalized, it.argb) }
    }

    fun colorCurso(argb: Long): Color = resolver(argb).acento(esOscuro)

    fun contenedorCurso(argb: Long): Color = resolver(argb).contenedor(esOscuro)

    fun sobreContenedorCurso(argb: Long): Color = resolver(argb).sobreContenedor(esOscuro)

    fun recursoNombreCurso(argb: Long): Int = resolver(argb).recursoNombre

    fun muestraCurso(argb: Long): MuestraCurso = resolver(argb)

    companion object {
        val Claro = PaletaCursos(esOscuro = false)
        val Oscuro = PaletaCursos(esOscuro = true)
    }
}

/** Paleta de curso del tema activo. */
val LocalPaletaCursos = staticCompositionLocalOf { PaletaCursos.Claro }

/** Color de acento de curso resuelto para el tema actual. */
@Composable
@ReadOnlyComposable
fun colorCurso(argb: Long): Color = LocalPaletaCursos.current.colorCurso(argb)

// ===========================================================================
// Tipografia
// ===========================================================================

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

/**
 * Escala tipográfica del DESIGN.md §3.2.
 *
 * El serif se reserva para títulos de pantalla; cuerpo, controles y contenido
 * editable usan sans.
 */
val Tipografia = Typography(
    displayLarge = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.50).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.20).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.20).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.10.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.10.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.10.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.10.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.10.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.40.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
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
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.20).sp,
    )

    val cuerpoEditor = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )

    val numerico = TextStyle(
        fontFamily = Sans,
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

/**
 * Formas del DESIGN.md §4.2.
 *
 * Mapeo a los slots de Material 3: extraSmall = 4, small = 8, medium = 12,
 * large = 16, extraLarge = 28. `full` no existe como slot y se usa CircleShape.
 */
val FormasNotally = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Acceso directo a los tokens de forma, para componentes que no usan slots. */
object FormaNotally {
    val xs = RoundedCornerShape(4.dp)
    val s = RoundedCornerShape(8.dp)
    val m = RoundedCornerShape(12.dp)
    val l = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(28.dp)
    val full = CircleShape

    /** Radio de fila: nunca 28 dp en una fila de lista. */
    val row = RoundedCornerShape(12.dp)
}

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

    // Springs de efectos: color, alpha, confirmación.
    fun <T> efectoNormal(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)

    fun <T> efectoRapida(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    // Duraciones puntuales (§5, excepciones permitidas).
    const val colorMs = 120
    const val dibujoMarcaMs = 160
    const val fabMs = 150
    const val chevronMs = 180
    const val movimientoElementoMs = 220
    const val fundidoMs = 180
    const val fundidoPestanaMs = 120
    const val slidePestanaMs = 180
    const val snackbarMs = 5_000L

    val fundidoEasing = FastOutSlowInEasing

    /** Atajo para `animateContentSize`. */
    fun tamanoContenido(): FiniteAnimationSpec<androidx.compose.ui.unit.IntSize> = espacialNormal()
}

// ===========================================================================
// Espaciado
// ===========================================================================

/** Espaciado del DESIGN.md §4.1 (múltiplos de 4). */
object EspaciadoNotally {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 24.dp
    val x6 = 32.dp
    val x7 = 48.dp

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
 * La paleta es fija: [EsquemaColoresClaro] o [EsquemaColoresOscuro] según el
 * modo elegido. No existe variante dinámica ni ajuste que altere los roles M3.
 */
@Composable
fun TemaNotally(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) EsquemaColoresOscuro else EsquemaColoresClaro
    val paletaCursos = if (darkTheme) PaletaCursos.Oscuro else PaletaCursos.Claro

    CompositionLocalProvider(LocalPaletaCursos provides paletaCursos) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Tipografia,
            shapes = FormasNotally,
            content = content,
        )
    }
}
