package me.kyuubiran.ncmdumper.ui.theme

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = Colors(
    primary = Pink40,
    primaryVariant = Pink80,
    secondary = PurpleGrey80,
    secondaryVariant = PurpleGrey80,
    background = Black,
    surface = Black,
    error = Red,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White,
    onError = White,
    isLight = false,
)

private val LightColorScheme = Colors(
    primary = Pink40,
    primaryVariant = Pink80,
    secondary = PurpleGrey80,
    secondaryVariant = PurpleGrey80,
    background = White,
    surface = White,
    error = Red,
    onPrimary = White,
    onSecondary = White,
    onBackground = Black,
    onSurface = Black,
    onError = White,
    isLight = true
)

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }


    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = if (darkTheme) colorScheme.surface else colorScheme.primary,
    )

    MaterialTheme(
        colors = colorScheme,
        typography = Typography,
        content = content
    )
}