package me.kyuubiran.ncmdumper.ui.fragments

import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kyuubiran.ncmdumper.R
import me.kyuubiran.ncmdumper.ui.views.NcmFileInfo
import me.kyuubiran.ncmdumper.ui.views.NcmFileItem
import java.io.File

object MainPage {
    private val list = mutableStateListOf<File>()

    private fun reloadFiles() {
        val sdCard = Environment.getExternalStorageDirectory()
        val cloudMusicDir =
            Uri.parse(sdCard.toString())
                .buildUpon()
                .appendPath("netease")
                .appendPath("cloudmusic")
                .appendPath("Music").build()

        val f = File(cloudMusicDir.toString())
        list.clear()
        if (!f.exists() || !f.isDirectory)
            return

        list += f.listFiles { ff ->
            ff.extension.lowercase() == "ncm"
//            true
        }?.toList() ?: emptyList()

        Log.i("MainFragment", "Found ${list.size} ncm files")
    }

    init {
        reloadFiles()
    }

    @Composable
    private fun AppBar(controller: NavHostController) {
        var moreShowed by remember { mutableStateOf(false) }
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = { Text(text = stringResource(id = R.string.app_name)) },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            moreShowed = !moreShowed
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Image(
                            modifier = Modifier.size(28.dp),
                            painter = painterResource(id = R.drawable.baseline_more_vert_24),
                            contentDescription = stringResource(id = R.string.settings),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                        DropdownMenu(
                            expanded = moreShowed,
                            onDismissRequest = { moreShowed = false },
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    // TODO: Search ui
                                    moreShowed = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.search))
                            }
                            DropdownMenuItem(
                                onClick = {
                                    controller.navigate("settings_page")
                                    moreShowed = false
                                }
                            ) {
                                Text(text = stringResource(id = R.string.settings))
                            }
                        }
                    }
                }
            },
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun NcmFileList() {
        val refreshScope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }

        fun refresh() = refreshScope.launch {
            refreshing = true
            val beg = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                reloadFiles()
            }
            val end = System.currentTimeMillis()
            if (end - beg < 500)
                delay(500 - (end - beg))

            refreshing = false
        }

        val state = rememberPullRefreshState(refreshing, ::refresh)
        Box(modifier = Modifier.pullRefresh(state)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list) { file ->
                    NcmFileItem(NcmFileInfo(file))
                }
            }

            if (list.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.nothing_here),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
        }
    }

    @Composable
    fun View(controller: NavHostController) {
        Column {
            AppBar(controller)
            NcmFileList()
        }
    }
}

