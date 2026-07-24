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
