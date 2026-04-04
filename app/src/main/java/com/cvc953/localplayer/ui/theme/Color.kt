package com.cvc953.localplayer.ui.theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// primary / Accent color for the app, used in sliders, icons, etc.
val md_primary = Color(0xFF2196F3)
val md_onPrimary = Color.White

// background color for surfaces like the player and settings cards
val md_background = Color.Black
val md_onBackground = Color.White

// Surface color for the main background of the app, used in the main activity and the player screen
val md_surface = Color(0xFF1A1A1A)
val md_onSurface = Color.White
val md_surfaceVariant = Color(0xFF404040)

// Text Variant colors for primary and secondary text on surfaces
val md_textSecondary = Color(0xFF808080)
val md_textPrimary = Color.White.copy(alpha = 0.7f)

// Overlay color for the player screen, used to darken the background when the controls are visible
val md_overlay = Color.Black.copy(alpha = 0.8f)

// Extra surface colors for cards and dialogs
val md_surfaceSheet = Color(0xFF121212)
val md_surfaceImagePlaceholder = Color(0xFFB0B0B0)

// Text variants (fine-gradient)
val md_textMeta = Color(0xFF808080)
val md_textSecondaryStrong = Color(0xFFAAAAAA)
val md_textSecondarySoft = Color(0xFFB0B0B0)

// Tracks / outlines
val md_outlineSoft = Color(0xFF404040)

// Gradients
val md_surfaceGradientTop = Color(0xFF0F0F0F)

// light Background
val md_lightBackground = Color(0xFFF5F5F5)
val md_lightOnBackground = Color(0xFF000000)

// light surfaces
val md_lightSurface = Color.White
val md_lightOnSurface = Color(0xFF1A1A1A)

// Variants / containers
val md_lightSurfaceVariant = Color(0xFFE0E0E0)
val md_lightSurfaceSheet = Color(0xFFFAFAFA)

// Text
val md_lightTextSecondary = Color(0xFF616161)
val md_lightTextSecondarySoft = Color(0xFF9E9E9E)
val md_lightTextSecondaryStrong = Color(0xFF424242)

// Text Variants
val md_textMeta_Light = Color(0xFF6F6F6F)

val md_surfaceSheet_Light = Color(0xFFE8E8E8)

// Outline
val md_lightOutlineSoft = Color(0xFFBDBDBD)

val dotsColorsDark =
    listOf<Color>(
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.4f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.6f),
        Color.White.copy(alpha = 0.7f),
        Color.White.copy(alpha = 0.8f),
        Color.White.copy(alpha = 0.9f),
    )
val dotsColorsLight =
    listOf<Color>(
        Color.Black.copy(alpha = 0.3f),
        Color.Black.copy(alpha = 0.4f),
        Color.Black.copy(alpha = 0.5f),
        Color.Black.copy(alpha = 0.6f),
        Color.Black.copy(alpha = 0.7f),
        Color.Black.copy(alpha = 0.8f),
        Color.Black.copy(alpha = 0.9f),
    )
val brightColorDark = Color.White
val brightColorLight = Color.Black

// ── Theme Color System ──────────────────────────────────────────────

const val DEFAULT_PRIMARY_HEX = "#2196F3"

data class ThemeColor(
    val name: String,
    val hex: String,
    val color: Color,
    val onColor: Color,
)

fun computeOnPrimary(color: Color): Color = if (color.luminance() > 0.3f) Color.Black else Color.White

fun resolvePrimaryColor(hex: String): ThemeColor =
    predefinedThemeColors.find { it.hex == hex }
        ?: predefinedThemeColors.first()

val predefinedThemeColors: List<ThemeColor> =
    listOf(
        ThemeColor("Azul", "#2196F3", Color(0xFF2196F3), computeOnPrimary(Color(0xFF2196F3))),
        ThemeColor("Rojo", "#F44336", Color(0xFFF44336), computeOnPrimary(Color(0xFFF44336))),
        ThemeColor("Verde", "#4CAF50", Color(0xFF4CAF50), computeOnPrimary(Color(0xFF4CAF50))),
        ThemeColor("Púrpura", "#9C27B0", Color(0xFF9C27B0), computeOnPrimary(Color(0xFF9C27B0))),
        ThemeColor("Naranja", "#FF9800", Color(0xFFFF9800), computeOnPrimary(Color(0xFFFF9800))),
        ThemeColor("Rosa", "#E91E63", Color(0xFFE91E63), computeOnPrimary(Color(0xFFE91E63))),
        ThemeColor("Teal", "#009688", Color(0xFF009688), computeOnPrimary(Color(0xFF009688))),
        ThemeColor("Índigo", "#3F51B5", Color(0xFF3F51B5), computeOnPrimary(Color(0xFF3F51B5))),
        ThemeColor("Ámbar", "#FFC107", Color(0xFFFFC107), computeOnPrimary(Color(0xFFFFC107))),
        ThemeColor("Cian", "#00BCD4", Color(0xFF00BCD4), computeOnPrimary(Color(0xFF00BCD4))),
    )
