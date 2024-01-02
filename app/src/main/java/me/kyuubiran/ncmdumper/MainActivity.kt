package me.kyuubiran.ncmdumper

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.kyuubiran.ncmdumper.ui.pages.MainPage
import me.kyuubiran.ncmdumper.ui.pages.SettingsPage
import me.kyuubiran.ncmdumper.ui.theme.MyTheme

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var sp: SharedPreferences
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences("settings", Activity.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            MyTheme {
                RequireAllFileAccessPermissionDialog(this)
                Box(modifier = Modifier.statusBarsPadding()) {
                    NavHost(navController = navController, startDestination = "main_page") {
                        composable("main_page") { MainPage.View(navController, this@MainActivity) }
                        composable("settings_page") { SettingsPage.View(navController, this@MainActivity) }
                    }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycle = LifecycleEventObserver { _, e ->
            if (e != Lifecycle.Event.ON_RESUME)
                return@LifecycleEventObserver

            if (Build.VERSION.SDK_INT < 30)
                return@LifecycleEventObserver

            if (showed) {
                showed = !Environment.isExternalStorageManager()
            }
        }

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
