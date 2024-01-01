package me.kyuubiran.ncmdumper

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.kyuubiran.ncmdumper.ui.fragments.MainPage
import me.kyuubiran.ncmdumper.ui.fragments.SettingsPage
import me.kyuubiran.ncmdumper.ui.theme.MyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            MyTheme {
                RequireAllFileAccessPermissionDialog(this)
                NavHost(navController = navController, startDestination = "main_page") {
                    composable("main_page") { MainPage.View(navController) }
                    composable("settings_page") { SettingsPage.View(navController) }
                }
            }
        }
    }
}

@Composable
private fun RequireAllFileAccessPermissionDialog(activity: Activity) {
    if (Build.VERSION.SDK_INT < 30)
        return

    var showed by remember { mutableStateOf(!Environment.isExternalStorageManager()) }

    val lifecycle = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            if (Build.VERSION.SDK_INT < 30)
                return

            if (showed) {
                showed = !Environment.isExternalStorageManager()
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(lifecycle)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycle) }
    }

    if (showed) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        activity.startActivity(intent)
                    },
                ) { Text(text = stringResource(id = R.string.confirm)) }
            },
            title = { Text(text = stringResource(id = R.string.title_dialog_require_all_file_access_permission)) },
            text = { Text(text = stringResource(id = R.string.message_dialog_require_all_file_access_permission)) },
        )
    }
}
