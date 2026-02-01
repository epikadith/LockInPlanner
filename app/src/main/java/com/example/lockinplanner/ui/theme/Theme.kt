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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.lockinplanner.domain.model.AppTheme

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
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System -> isSystemInDarkTheme()
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