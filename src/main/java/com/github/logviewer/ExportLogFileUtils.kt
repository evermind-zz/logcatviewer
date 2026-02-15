package com.github.logviewer

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
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

class ExportLogFileUtils {

    private suspend fun exportLogs(cacheDir: File?, logs: Array<LogItem>?): File? =
        withContext(Dispatchers.IO) {
            if (cacheDir == null || cacheDir.isFile || logs.isNullOrEmpty()) {
                null
            } else {
                val logFilePrefix = Settings.config.logFilePrefix.getPrefix()
                val logFile = File(cacheDir, "${logFilePrefix}.log")
                if (logFile.exists() && !logFile.delete()) {
                    null
                } else {
                    try {
                        val writer = BufferedWriter(
                            OutputStreamWriter(
                                FileOutputStream(logFile)
                            )
                        )

                        Settings.config.logfileFormat.writeLogs(logFile.name, logs, writer)

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
        val exportedFile = exportLogs(context.externalCacheDir, logData)

        if (exportedFile == null) {
            showFeedback(
                rootView,
                R.string.logcat_viewer_create_log_file_failed,
            )
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
                showFeedback(
                    rootView,
                    R.string.logcat_viewer_not_support_on_this_device
                )
            } else {
                context.startActivity(shareIntent)
            }
        }
    }

    private fun showFeedback(view: View, @StringRes resId: Int) {
        if (canShowSnackbar(view)) {
            Snackbar.make(view, resId, Snackbar.LENGTH_LONG).show()
        } else {
            // Fallback for Overlays
            Toast.makeText(view.context.applicationContext, resId, Toast.LENGTH_LONG).show()
        }
    }

    private fun canShowSnackbar(view: View): Boolean {
        if (!view.isAttachedToWindow) return false

        var current: View? = view
        while (current != null) {
            // CoordinatorLayout is your best parent
            if (current is androidx.coordinatorlayout.widget.CoordinatorLayout) return true
            // the standard root view of an activity
            if (current.id == android.R.id.content) return true

            val parent = current.parent
            current = parent as? View
        }
        return false
    }
}
