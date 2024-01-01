package me.kyuubiran.ncmdumper.ui.pages

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import me.kyuubiran.ncmdumper.BuildConfig
import me.kyuubiran.ncmdumper.MainActivity
import me.kyuubiran.ncmdumper.R
import me.kyuubiran.ncmdumper.ui.utils.Dumper

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
    private fun Configs(activity: MainActivity) {
        val sp = activity.sp
        var outputFolder by remember { mutableStateOf(sp.getString("output_path", Dumper.DEFAULT_NETEASE_MUSIC_PATH)!!) }

        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data?.path
                if (uri != null) {
                    val realPath = Environment.getExternalStorageDirectory().path + "/" +
                            uri.substring(uri.indexOf(':') + 1)

                    Log.i("SettingsPage", "Set output path: $realPath")
                    sp.edit().putString("output_path", realPath).apply()
                    outputFolder = realPath
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // region Dumper
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                text = stringResource(id = R.string.title_dumper),
                color = MaterialTheme.colors.primary,
                fontSize = 18.sp
            )

            SwitchConfigItem(
                title = stringResource(id = R.string.title_config_search_full_disk),
                subtitle = stringResource(id = R.string.subtitle_config_search_full_disk),
                isChecked = false,
                onChanged = {
                    sp.edit().putBoolean("search_full_disk", it).apply()
                }
            )

            BaseConfigItem(
                title = stringResource(id = R.string.title_config_set_output_folder),
                subtitle = outputFolder,
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    folderPicker.launch(intent)
                }
            )
            // endregion

            // region about
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                text = stringResource(id = R.string.title_about),
                color = MaterialTheme.colors.primary,
                fontSize = 18.sp
            )
            BaseConfigItem(
                title = stringResource(id = R.string.title_version),
                subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
            BaseConfigItem(
                title = stringResource(id = R.string.title_config_goto_github_page),
                subtitle = stringResource(id = R.string.subtitle_config_goto_github_page),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://github.com/KyuubiRan/NcmDumperApp")
                    activity.startActivity(intent)
                }
            )
            // endregion
        }
    }

    @Composable
    fun View(navController: NavHostController, activity: MainActivity) {
        Column {
            AppBar(navController = navController)
            Configs(activity = activity)
        }
    }
}

@Composable
private fun BaseConfigItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    rightView: @Composable () -> Unit = {
        Text(text = ">", fontSize = 20.sp, color = Color.Gray)
    }
) {
    Box(modifier = Modifier
        .apply { if (onClick != null) clickable(onClick = onClick) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = 60.dp)
                    .fillMaxWidth(.8f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 18.sp)
                if (!subtitle.isNullOrBlank())
                    Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(alignment = Alignment.CenterVertically)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                rightView()
            }
        }
    }
}

@Composable
private fun SwitchConfigItem(
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onChanged: (Boolean) -> Unit = {},
) {
    var checked by remember { mutableStateOf(isChecked) }

    BaseConfigItem(
        title = title,
        subtitle = subtitle,
        onClick = { checked = !checked },
        rightView = {
            Switch(checked = checked, onCheckedChange = {
                checked = !checked
                onChanged(checked)
            })
        }
    )
}

@Preview
@Composable
private fun TestBaseConfigItem() {
    Column {
        BaseConfigItem(title = "标题", subtitle = "测试", onClick = { })
        SwitchConfigItem(title = "测试2", isChecked = true, onChanged = { })
    }
}