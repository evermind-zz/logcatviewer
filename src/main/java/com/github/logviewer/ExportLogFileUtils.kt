package com.github.logviewer

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportLogFileUtils {

    private suspend fun exportLogs(cacheDir: File?, logs: Array<LogItem>?): File? =
        withContext(Dispatchers.IO) {
            if (cacheDir == null || cacheDir.isFile() || logs.isNullOrEmpty()) {
                null
            } else {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                val logFile = File(cacheDir, dateFormat.format(Date()) + ".log")
                if (logFile.exists() && !logFile.delete()) {
                    null
                } else {
                    try {
                        val writer = BufferedWriter(
                            OutputStreamWriter(
                                FileOutputStream(logFile)
                            )
                        )
                        for (log in logs) {
                            writer.write(log.origin + "\n")
                        }

                        writer.close()
                        logFile
                    } catch (e: IOException) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }

    fun exportLog(
        context: Context,
        logData: Array<LogItem>,
        rootView: View,
        coroutineScope: CoroutineScope // should run on the UI-Thread
    ) {
        coroutineScope.launch {
            exportLog(context, logData, rootView)
        }
    }

    suspend fun exportLog(
        context: Context,
        logData: Array<LogItem>,
        rootView: View
    ) {
        val exportedFile = withContext(Dispatchers.IO) {
            exportLogs(context.externalCacheDir, logData)
        }

        if (exportedFile == null) {
            Snackbar.make(
                rootView,
                R.string.logcat_viewer_create_log_file_failed,
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                type = "text/plain"
                val uri = LogcatFileProvider.getUriForFile(
                    context,
                    "${context.packageName}.logcat_fileprovider",
                    exportedFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
            }

            if (context.packageManager.queryIntentActivities(shareIntent, 0).isEmpty()) {
                Snackbar.make(
                    rootView,
                    R.string.logcat_viewer_not_support_on_this_device,
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                context.startActivity(shareIntent)
            }
        }
    }
}
