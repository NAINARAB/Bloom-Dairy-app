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
    "royal" to GradientScheme(listOf(Color(0xFF141E30), Color(0xFF243B55)), listOf(Color(0xFF0A0F18), Color(0xFF101B28)), "Royal")
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

private val BloomTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
)

@Composable
fun BloomTheme(
    themeMode: String = "system",   // system | light | dark
    accentKey: String = "violet",
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
        typography = BloomTypography,
        content = content,
    )
}
