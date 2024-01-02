package me.kyuubiran.ncmdumper.ui.pages

import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import me.kyuubiran.ncmdumper.MainActivity
import me.kyuubiran.ncmdumper.R
import me.kyuubiran.ncmdumper.ui.theme.BackGroundColor
import me.kyuubiran.ncmdumper.ui.theme.LightBlue
import me.kyuubiran.ncmdumper.ui.theme.LightDark
import me.kyuubiran.ncmdumper.ui.utils.Dumper
import me.kyuubiran.ncmdumper.ui.views.NcmFileInfo
import me.kyuubiran.ncmdumper.ui.views.NcmFileItem
import java.io.File

object MainPage {
    private val list = mutableStateListOf<File>()
    private val listFiltered = mutableStateListOf<File>()

    private fun reloadFiles() {
        val f = File(Dumper.DEFAULT_NETEASE_MUSIC_PATH)
        list.clear()
        if (!f.exists() || !f.isDirectory)
            return

        list += f.listFiles { ff -> ff.extension.lowercase() == "ncm" }?.toList() ?: emptyList()

        Log.i("MainFragment", "Found ${list.size} ncm files")
    }

    private fun reloadFilesFullDisk() {
        val f = Environment.getExternalStorageDirectory()

        list.clear()

        fun doSearch(f: File) {
            if (!f.exists())
                return

            if (f.isDirectory) {
                f.listFiles()?.forEach {
                    doSearch(it)
                }
            } else {
                if (f.extension.lowercase() == "ncm")
                    list += f
            }
        }

        doSearch(f)
    }

    private fun updateSearch(str: String) {
        if (!showSearch.value)
            return

        listFiltered.clear()
        if (str.isBlank()) {
            listFiltered += list
            return
        }

        val args = str.split(' ')
        list.forEach {
            listFiltered.apply {
                val name = it.name
                if (args.all { arg -> name.contains(arg, ignoreCase = true) })
                    add(it)
            }
        }
    }

    @Composable
    private fun AppBar(controller: NavHostController) {
        var moreShowed by remember { mutableStateOf(false) }
        TopAppBar(
            title = {
                if (!showSearch.value) {
                    Text(text = stringResource(id = R.string.app_name))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showSearch.value = false },
                            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                            contentDescription = stringResource(id = R.string.back),
                            colorFilter = ColorFilter.tint(Color.White)
                        )

                        var text by remember { mutableStateOf("") }
                        val selection = TextSelectionColors(
                            handleColor = LightBlue,
                            backgroundColor = Color.DarkGray,
                        )
                        CompositionLocalProvider(LocalTextSelectionColors provides selection) {
                            SelectionContainer {
                                OutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp),
                                    value = text,
                                    onValueChange = {
                                        text = it
                                        updateSearch(it)
                                    },
                                    placeholder = {
                                        Text(text = stringResource(id = R.string.search_with_ellipsis), color = Color.Gray)
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        cursorColor = Color.White)
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                if (!showSearch.value)
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
                                        showSearch.value = true
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
        val sp = MainActivity.sp

        val refreshScope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }

        fun refresh() = refreshScope.launch {
            refreshing = true
            val beg = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                if (sp.getBoolean("search_full_disk", false))
                    reloadFilesFullDisk()
                else
                    reloadFiles()

                listFiltered.clear()
                listFiltered += list
            }
            val end = System.currentTimeMillis()
            if (end - beg < 500)
                delay(500 - (end - beg))

            refreshing = false
        }

        LaunchedEffect("refresh") {
            refresh()
        }

        val state = rememberPullRefreshState(refreshing, ::refresh)
        Box(
            modifier = Modifier
                .pullRefresh(state)
                .background(BackGroundColor)
        ) {
            val showList = if (showSearch.value)
                listFiltered
            else
                list

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(showList) { file ->
                    NcmFileItem(NcmFileInfo(file))
                }
            }

            if (showList.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.nothing_here),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
        }
    }

    private val showSearch = mutableStateOf(false)

    @Composable
    fun View(controller: NavHostController, activity: MainActivity) {
        BackHandler(enabled = showSearch.value) {
            showSearch.value = false
        }

        Column {
            AppBar(controller)
            NcmFileList()
        }
    }
}

