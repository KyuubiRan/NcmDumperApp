package me.kyuubiran.ncmdumper.ui.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.kyuubiran.ncmdumper.R

object SettingsPage {
    @Composable
    private fun AppBar(navController: NavHostController) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = {
                Row {
                    Image(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navController.popBackStack() },
                        painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                        contentDescription = stringResource(id = R.string.back),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Text(modifier = Modifier.padding(start = 8.dp), text = stringResource(id = R.string.settings))
                }
            },
        )
    }

    @Composable
    fun View(navController: NavHostController) {
        Column {
            AppBar(navController = navController)
        }
    }
}