package me.kyuubiran.ncmdumper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val LightDark = Color(0xFF1E1E1E)
val LightBlue = Color(0xFF1E88E5)

val BackGroundColor
    @Composable get() = if (isSystemInDarkTheme()) LightDark else Color.White
