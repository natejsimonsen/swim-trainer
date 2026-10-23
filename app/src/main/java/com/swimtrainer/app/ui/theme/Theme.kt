package com.swimtrainer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.swimtrainer.core.model.Zone

private val Light = lightColorScheme(
    primary = Color(0xFF006A8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2E8FF),
    onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F1E8),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFFF7043),
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF3A0B00),
    background = Color(0xFFF5FBFF),
    surface = Color(0xFFF5FBFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF75D1FF),
    onPrimary = Color(0xFF00354B),
    primaryContainer = Color(0xFF004D6B),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = Color(0xFF6FD6C6),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFB2F1E8),
    tertiary = Color(0xFFFFB59E),
    tertiaryContainer = Color(0xFF7A2E14),
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFF0F1417),
    surface = Color(0xFF0F1417),
)

@Composable
fun SwimTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Consistent colour per intensity zone, used for chips and the pace clock. */
fun zoneColor(zone: Zone): Color = when (zone) {
    Zone.RECOVERY -> Color(0xFF78909C)
    Zone.AEROBIC -> Color(0xFF29B6F6)
    Zone.TEMPO -> Color(0xFF26A69A)
    Zone.THRESHOLD -> Color(0xFFFFA726)
    Zone.VO2 -> Color(0xFFEF5350)
    Zone.SPRINT -> Color(0xFFAB47BC)
}
