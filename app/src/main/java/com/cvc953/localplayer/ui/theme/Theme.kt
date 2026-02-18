import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MovableContent
import com.cvc953.localplayer.ui.theme.md_background
import com.cvc953.localplayer.ui.theme.md_lightBackground
import com.cvc953.localplayer.ui.theme.md_lightOnBackground
import com.cvc953.localplayer.ui.theme.md_lightOnSurface
import com.cvc953.localplayer.ui.theme.md_lightOutlineSoft
import com.cvc953.localplayer.ui.theme.md_lightSurfaceVariant
import com.cvc953.localplayer.ui.theme.md_lightTextSecondary
import com.cvc953.localplayer.ui.theme.md_onBackground
import com.cvc953.localplayer.ui.theme.md_onPrimary
import com.cvc953.localplayer.ui.theme.md_onSurface
import com.cvc953.localplayer.ui.theme.md_outlineSoft
import com.cvc953.localplayer.ui.theme.md_primary
import com.cvc953.localplayer.ui.theme.md_surface
import com.cvc953.localplayer.ui.theme.md_surfaceSheet
import com.cvc953.localplayer.ui.theme.md_textSecondary

private val DarkColorScheme =
    darkColorScheme(
        // primary / accent
        primary = md_primary,
        onPrimary = md_onPrimary,
        // background
        background = md_background,
        onBackground = md_onBackground,
        // Main surfaces
        surface = md_surface,
        onSurface = md_onSurface,
        // variants & outlines
        outline = md_outlineSoft,
        surfaceVariant = md_surfaceSheet,
        onSurfaceVariant = md_textSecondary,
    )

private val LightColorScheme =
    lightColorScheme(
        // primary
        primary = md_primary,
        onPrimary = md_onPrimary,
        // Background
        background = md_lightBackground,
        onBackground = md_lightOnBackground,
        // Surfaces
        surface = md_lightOnSurface,
        onSurface = md_lightOnSurface,
        // Variants
        surfaceVariant = md_lightSurfaceVariant,
        onSurfaceVariant = md_lightTextSecondary,
        // Outline / dividers / inactive tracks
        outline = md_lightOutlineSoft,
    )

@Suppress("ktlint:standard:function-naming")
@Composable
fun LocalPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
