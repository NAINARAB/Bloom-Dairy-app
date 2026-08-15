package mobile.dairy.app.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Bloom design language — "quiet paper".
 * Warm paper-white base, dusk-violet default accent, deep ink text.
 * The same five user-selectable accents as the shared schema
 * (prefs/app.accent): violet | teal | rose | amber | sky.
 */

data class Accent(val light: Color, val dark: Color, val label: String)

val ACCENTS: Map<String, Accent> = mapOf(
    "violet" to Accent(Color(0xFF6D5BD0), Color(0xFF8B7BE8), "Dusk violet"),
    "teal" to Accent(Color(0xFF2E8F84), Color(0xFF4FB3A7), "Deep teal"),
    "rose" to Accent(Color(0xFFC75D7E), Color(0xFFDE7F9E), "Quiet rose"),
    "amber" to Accent(Color(0xFFB57E24), Color(0xFFD9A44A), "Warm amber"),
    "sky" to Accent(Color(0xFF3D7BC4), Color(0xFF6BA3E0), "Open sky"),
    "emerald" to Accent(Color(0xFF10B981), Color(0xFF34D399), "Emerald"),
    "coral" to Accent(Color(0xFFE05A47), Color(0xFFF87171), "Warm coral"),
    "lavender" to Accent(Color(0xFF8B5CF6), Color(0xFFA78BFA), "Lavender"),
    "indigo" to Accent(Color(0xFF4F46E5), Color(0xFF818CF8), "Deep indigo"),
    "mint" to Accent(Color(0xFF059669), Color(0xFF10B981), "Fresh mint"),
    "crimson" to Accent(Color(0xFFDC2626), Color(0xFFEF4444), "Crimson"),
    "gold" to Accent(Color(0xFFD97706), Color(0xFFFBBF24), "Radiant gold"),
)

data class GradientScheme(val light: List<Color>, val dark: List<Color>, val label: String)

val GRADIENTS: Map<String, GradientScheme> = mapOf(
    "midnight" to GradientScheme(listOf(Color(0xFF533483), Color(0xFF16213E)), listOf(Color(0xFF2B1B4D), Color(0xFF0B1120)), "Midnight"),
    "sunset" to GradientScheme(listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), listOf(Color(0xFFB34233), Color(0xFFB57042)), "Sunset"),
    "ocean" to GradientScheme(listOf(Color(0xFF2B5876), Color(0xFF4E4376)), listOf(Color(0xFF112635), Color(0xFF261F3B)), "Ocean"),
    "aurora" to GradientScheme(listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), listOf(Color(0xFF00586B), Color(0xFF003C52)), "Aurora"),
    "forest" to GradientScheme(listOf(Color(0xFF1D976C), Color(0xFF93F9B9)), listOf(Color(0xFF0E4C36), Color(0xFF457958)), "Forest"),
    "berry" to GradientScheme(listOf(Color(0xFFB224EF), Color(0xFF7579FF)), listOf(Color(0xFF561175), Color(0xFF393B7D)), "Berry"),
    "ember" to GradientScheme(listOf(Color(0xFFF12711), Color(0xFFF5AF19)), listOf(Color(0xFF751207), Color(0xFF78550B)), "Ember"),
    "royal" to GradientScheme(listOf(Color(0xFF141E30), Color(0xFF243B55)), listOf(Color(0xFF0A0F18), Color(0xFF101B28)), "Royal"),
    "nebula" to GradientScheme(listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)), listOf(Color(0xFF240073), Color(0xFF471770)), "Nebula"),
    "peach" to GradientScheme(listOf(Color(0xFFED4264), Color(0xFFFFEDBC)), listOf(Color(0xFF7A1D30), Color(0xFF7E7259)), "Peach"),
    "lush" to GradientScheme(listOf(Color(0xFF56AB2F), Color(0xFFA8E063)), listOf(Color(0xFF2A5417), Color(0xFF516E30)), "Lush"),
    "cosmic" to GradientScheme(listOf(Color(0xFF1D2671), Color(0xFFC33764)), listOf(Color(0xFF0E1339), Color(0xFF611C32)), "Cosmic"),
    "dusk" to GradientScheme(listOf(Color(0xFF2C3E50), Color(0xFFFD746C)), listOf(Color(0xFF151E28), Color(0xFF7E3935)), "Dusk"),
    "twilight" to GradientScheme(listOf(Color(0xFF0F2027), Color(0xFF2C5364)), listOf(Color(0xFF070F13), Color(0xFF162A32)), "Twilight")
)

// Semantic extras that Material3's scheme doesn't carry
object BloomColors {
    val successLight = Color(0xFF3E9D6E)
    val successDark = Color(0xFF54B586)
    val warningLight = Color(0xFFC08A2D)
    val warningDark = Color(0xFFD9A44A)

    @Composable
    fun success(): Color = if (isSystemInDarkTheme()) successDark else successLight

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) warningDark else warningLight
}

private fun lightScheme(accent: Accent): ColorScheme = lightColorScheme(
    primary = accent.light,
    onPrimary = Color.White,
    primaryContainer = accent.light.copy(alpha = 0.08f),
    onPrimaryContainer = accent.light,
    background = Color(0xFFFDFCF9), // Warmer, more paper-like
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCF9),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFC4C6D0),
    outlineVariant = Color(0xFFE1E2EC),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private fun darkScheme(accent: Accent): ColorScheme = darkColorScheme(
    primary = accent.dark,
    onPrimary = Color(0xFF1A1C1E),
    primaryContainer = accent.dark.copy(alpha = 0.12f),
    onPrimaryContainer = accent.dark,
    background = Color(0xFF0E1113), // Soft dark, not pure black
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF44474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

fun bloomTypography(baseSize: Int = 14, boldText: Boolean = false): Typography {
    val scale = (baseSize.toFloat() / 14f).coerceIn(0.8f, 1.35f)
    val regularWeight = if (boldText) FontWeight.SemiBold else FontWeight.Normal
    val mediumWeight = if (boldText) FontWeight.Bold else FontWeight.Medium
    val semiBoldWeight = if (boldText) FontWeight.ExtraBold else FontWeight.SemiBold
    val boldWeight = if (boldText) FontWeight.Black else FontWeight.Bold
    val extraBoldWeight = if (boldText) FontWeight.Black else FontWeight.ExtraBold

    return Typography(
        displaySmall = TextStyle(fontSize = (28 * scale).sp, fontWeight = extraBoldWeight, letterSpacing = (-0.8).sp),
        headlineMedium = TextStyle(fontSize = (22 * scale).sp, fontWeight = boldWeight, letterSpacing = (-0.4).sp),
        headlineSmall = TextStyle(fontSize = (19 * scale).sp, fontWeight = boldWeight, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontSize = (17 * scale).sp, fontWeight = boldWeight, letterSpacing = (-0.2).sp),
        titleMedium = TextStyle(fontSize = (15 * scale).sp, fontWeight = semiBoldWeight),
        titleSmall = TextStyle(fontSize = (13 * scale).sp, fontWeight = mediumWeight),
        bodyLarge = TextStyle(fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp, fontWeight = regularWeight, letterSpacing = 0.15.sp),
        bodyMedium = TextStyle(fontSize = (12.5 * scale).sp, lineHeight = (18 * scale).sp, fontWeight = regularWeight, letterSpacing = 0.15.sp),
        bodySmall = TextStyle(fontSize = (11 * scale).sp, lineHeight = (15 * scale).sp, fontWeight = regularWeight),
        labelLarge = TextStyle(fontSize = (12 * scale).sp, fontWeight = semiBoldWeight),
        labelMedium = TextStyle(fontSize = (10.5 * scale).sp, fontWeight = boldWeight, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontSize = (9.5 * scale).sp, fontWeight = boldWeight, letterSpacing = 0.6.sp),
    )
}

@Composable
fun BloomTheme(
    themeMode: String = "system",   // system | light | dark
    accentKey: String = "violet",
    fontSize: Int = 14,             // 12..18 px/sp
    boldText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = ACCENTS[accentKey] ?: ACCENTS.getValue("violet")
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = if (dark) darkScheme(accent) else lightScheme(accent),
        typography = bloomTypography(fontSize, boldText),
        content = content,
    )
}
