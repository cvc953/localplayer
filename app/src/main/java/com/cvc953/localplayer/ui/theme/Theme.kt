import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.cvc953.localplayer.ui.theme.ExtendedColors
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.ui.theme.computeOnPrimary
import com.cvc953.localplayer.ui.theme.md_background
import com.cvc953.localplayer.ui.theme.md_lightBackground
import com.cvc953.localplayer.ui.theme.md_lightOnBackground
import com.cvc953.localplayer.ui.theme.md_lightOnSurface
import com.cvc953.localplayer.ui.theme.md_lightOutlineSoft
import com.cvc953.localplayer.ui.theme.md_lightSurface
import com.cvc953.localplayer.ui.theme.md_lightSurfaceVariant
import com.cvc953.localplayer.ui.theme.md_lightTextSecondary
import com.cvc953.localplayer.ui.theme.md_lightTextSecondarySoft
import com.cvc953.localplayer.ui.theme.md_lightTextSecondaryStrong
import com.cvc953.localplayer.ui.theme.md_onBackground
import com.cvc953.localplayer.ui.theme.md_onSurface
import com.cvc953.localplayer.ui.theme.md_outlineSoft
import com.cvc953.localplayer.ui.theme.md_surface
import com.cvc953.localplayer.ui.theme.md_surfaceSheet
import com.cvc953.localplayer.ui.theme.md_surfaceSheet_Light
import com.cvc953.localplayer.ui.theme.md_textMeta
import com.cvc953.localplayer.ui.theme.md_textMeta_Light
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.ui.theme.md_textSecondarySoft
import com.cvc953.localplayer.ui.theme.md_textSecondaryStrong

@Suppress("ktlint:standard:function-naming")
@Composable
fun LocalPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = Color(0xFF2196F3),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        remember(darkTheme, primaryColor) {
            val onPrimary = computeOnPrimary(primaryColor)
            if (darkTheme) {
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    background = md_background,
                    onBackground = md_onBackground,
                    surface = md_surface,
                    onSurface = md_onSurface,
                    outline = md_outlineSoft,
                    surfaceVariant = md_surfaceSheet,
                    onSurfaceVariant = md_textSecondary,
                )
            } else {
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    background = md_lightBackground,
                    onBackground = md_lightOnBackground,
                    surface = md_lightSurface,
                    onSurface = md_lightOnSurface,
                    surfaceVariant = md_lightSurfaceVariant,
                    onSurfaceVariant = md_lightTextSecondary,
                    outline = md_lightOutlineSoft,
                )
            }
        }

    val extendedColors =
        if (darkTheme) {
            ExtendedColors(
                textSecondary = md_textSecondary,
                textSecondarySoft = md_textSecondarySoft,
                textSecondaryStrong = md_textSecondaryStrong,
                texMeta = md_textMeta,
                surfaceSheet = md_surfaceSheet,
            )
        } else {
            ExtendedColors(
                textSecondary = md_lightTextSecondary,
                textSecondarySoft = md_lightTextSecondarySoft,
                textSecondaryStrong = md_lightTextSecondaryStrong,
                texMeta = md_textMeta_Light,
                surfaceSheet = md_surfaceSheet_Light,
            )
        }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
