package com.example.lockinplanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.lockinplanner.domain.model.AppTheme
import com.example.lockinplanner.domain.model.UserPreferences
import androidx.core.graphics.ColorUtils

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun LockInPlannerTheme(
    appTheme: AppTheme = AppTheme.System,
    userPreferences: UserPreferences? = null,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System, AppTheme.Custom -> isSystemInDarkTheme() 
        else -> isSystemInDarkTheme() // Default for colored themes unless specified otherwise
    }

    val colorScheme = when (appTheme) {
        AppTheme.Red -> lightColorScheme(
            primary = Color(0xFFD32F2F),
            secondary = Color(0xFFC62828),
            tertiary = Color(0xFFB71C1C)
        )
        AppTheme.Green -> lightColorScheme(
            primary = Color(0xFF388E3C),
            secondary = Color(0xFF2E7D32),
            tertiary = Color(0xFF1B5E20)
        )
        AppTheme.Blue -> lightColorScheme(
            primary = Color(0xFF1976D2),
            secondary = Color(0xFF1565C0),
            tertiary = Color(0xFF0D47A1)
        )
        AppTheme.Yellow -> lightColorScheme(
            primary = Color(0xFFFBC02D),
            secondary = Color(0xFFF9A825),
            tertiary = Color(0xFFF57F17)
        )
        AppTheme.Orange -> lightColorScheme(
            primary = Color(0xFFE64A19),
            secondary = Color(0xFFD84315),
            tertiary = Color(0xFFBF360C)
        )
        AppTheme.Olive -> darkColorScheme(
            primary = Color(0xFF558B2F),
            secondary = Color(0xFF33691E),
            tertiary = Color(0xFF1B5E20)
        )
        AppTheme.Navy -> darkColorScheme(
            primary = Color(0xFF1565C0),
            secondary = Color(0xFF0D47A1),
            tertiary = Color(0xFF002171)
        )
        AppTheme.Custom -> {
            val primaryColor = try {
                userPreferences?.customPrimaryColor?.let { Color(it.toULong()).also { c -> c.toArgb() } } ?: Purple40
            } catch (e: Throwable) { Purple40 }
            
            val secondaryColor = try {
                userPreferences?.customSecondaryColor?.let { Color(it.toULong()).also { c -> c.toArgb() } } ?: PurpleGrey40
            } catch (e: Throwable) { PurpleGrey40 }
            
            // Mix foreground and background colors slightly to create a cohesive tertiary accent
            val tertiaryColor = try {
                Color(ColorUtils.blendARGB(primaryColor.toArgb(), secondaryColor.toArgb(), 0.3f))
            } catch (e: Throwable) { Pink40 }
            
            // Reassign the "Background" to Primary. 
            // Reassign the "Primary Object/Text Color" to Secondary for contrast.
            if (darkTheme) {
                darkColorScheme(
                    background = primaryColor,
                    surface = primaryColor,
                    primary = secondaryColor,
                    onPrimary = primaryColor,
                    onBackground = secondaryColor,
                    onSurface = secondaryColor,
                    tertiary = tertiaryColor,
                    onTertiary = primaryColor
                )
            } else {
                lightColorScheme(
                    background = primaryColor,
                    surface = primaryColor,
                    primary = secondaryColor,
                    onPrimary = primaryColor,
                    onBackground = secondaryColor,
                    onSurface = secondaryColor,
                    tertiary = tertiaryColor,
                    onTertiary = primaryColor
                )
            }
        }
        else -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}