# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces in release
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Media3 (ExoPlayer)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Palette
-keep class androidx.palette.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }