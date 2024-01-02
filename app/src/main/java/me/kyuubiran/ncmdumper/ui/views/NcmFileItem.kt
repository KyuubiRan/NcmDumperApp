package me.kyuubiran.ncmdumper.ui.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kyuubiran.ncmdumper.R
import me.kyuubiran.ncmdumper.ui.utils.Dumper.dumpNcmFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("SimpleDateFormat", "ConstantLocale")
val fmt = SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())

data class NcmFileInfo(val file: File) {
    val fileName: String = file.name
    val filePath: String = file.path
    val fileSize = file.length()
    val createDate: String = fmt.format(file.lastModified())
}

@Composable
private fun ConfirmDumpDialog(show: Boolean, fileInfo: NcmFileInfo, onDismiss: () -> Unit) {
    val sp = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var exportResult by remember { mutableIntStateOf(-1) }
    var exportingDialogShow by remember { mutableStateOf(false) }
    var exportResultDialogShow by remember { mutableStateOf(false) }
    val execScope = rememberCoroutineScope()
    fun exec() {
        execScope.launch {
            exportingDialogShow = true
            withContext(Dispatchers.IO) {
                val beg = System.currentTimeMillis()
                val output = sp.getString("output_path", fileInfo.file.parent ?: "") ?: ""
                exportResult = dumpNcmFile(fileInfo.filePath, output)
                val end = System.currentTimeMillis()
                if (end - beg < 500) delay(500 - (end - beg))
            }
            exportingDialogShow = false
            exportResultDialogShow = true
        }
    }

    if (show)
        AlertDialog(onDismissRequest = onDismiss,
            buttons = {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = {
                        exec()
                        onDismiss()
                    }) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }
            },
            title = {
                Text(text = stringResource(id = R.string.title_dialog_confirm_dump))
            },
            text = {
                Text(
                    text = stringResource(id = R.string.message_dialog_confirm_dump).format(fileInfo.fileName)
                )
            })

    if (exportingDialogShow)
        AlertDialog(
            onDismissRequest = { },
            buttons = {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        CircularProgressIndicator()
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringResource(id = R.string.message_dialog_exporting)
                        )
                    }
                }
            }
        )

    if (exportResultDialogShow)
        AlertDialog(onDismissRequest = { exportResultDialogShow = false },
            buttons = {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { exportResultDialogShow = false }) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }
            },
            title = {
                Text(text = stringResource(id = if (exportResult == 0) R.string.title_dialog_dump_success else R.string.title_dialog_dump_failed))
            },
            text = {
                val resId = when (exportResult) {
                    0 -> R.string.message_dialog_dump_success
                    1 -> R.string.message_dialog_dump_failed_invalid_input_file
                    2 -> R.string.message_dialog_dump_failed_invalid_output_folder
                    3 -> R.string.message_dialog_dump_failed_file_not_ncm_file
                    4 -> R.string.message_dialog_dump_failed_file_unknow_format
                    5 -> R.string.message_dialog_dump_failed_file_cannot_read_music_info
                    6 -> R.string.message_dialog_dump_failed_file_cannot_read_music_cover
                    7 -> R.string.message_dialog_dump_failed_file_cannot_read_music_data
                    8 -> R.string.message_dialog_dump_failed_file_cannot_save_output_file
                    else -> R.string.message_dialog_dump_failed_unknown
                }
                Text(text = stringResource(id = resId))
            })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NcmFileItem(fileInfo: NcmFileInfo) {
    var dialogShow by remember { mutableStateOf(false) }
    ConfirmDumpDialog(show = dialogShow, fileInfo = fileInfo) {
        dialogShow = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Box(modifier = Modifier.clickable { dialogShow = true }) {
            Column(
                modifier = Modifier
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    text = fileInfo.fileName,
                    fontSize = 18.sp
                )
                Text(
                    maxLines = 1,
                    text = "${fileInfo.createDate} | ${String.format("%.2f", fileInfo.fileSize / 1024.00 / 1024.00)}MB",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}