package com.github.logviewer

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportLogFileUtils(val settings: Settings = Settings.Default) {
    /**
     * [CACHE_INTERNAL] and [CACHE_EXTERNAL] are exposed via the [LogcatFileProvider]'s XML.
     */
    enum class StorageLocation {
        /** [Context.externalCacheDir] */
        CACHE_EXTERNAL,

        /**  [Context.cacheDir] */
        CACHE_INTERNAL
    }

    private suspend fun writeLogDataToFileInternal(logDir: File?, logs: Array<LogItem>?): File? =
        withContext(Dispatchers.IO) {
            if (logDir == null || logDir.isFile || logs.isNullOrEmpty()) {
                return@withContext null
            }

            val logFile = File(logDir, settings.config.logFileName.getLogFileName())

            if (logFile.exists() && !logFile.delete()) {
                return@withContext null
            }

            runCatching {
                logFile.writeLogsSecurely(logs, settings.config.logfileFormat)
                logFile
            }.onFailure { e ->
                e.printStackTrace()
            }.getOrNull()
        }

    suspend fun writeLogDataToFile(context: Context, logs: Array<LogItem>?): File? {
        val logDir = settings.config.getLogFolder(context)
        logDir?.let {
            settings.config.logCleanupStrategy.applyStrategy(logDir)
        }
        return writeLogDataToFileInternal(logDir, logs)
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
        val exportedFile = writeLogDataToFile(context, logData)

        if (exportedFile == null) {
            showFeedback(
                rootView,
                R.string.logcat_viewer_create_log_file_failed,
            )
        } else {
            val logUri = LogcatFileProvider.getUriForFile(
                context,
                LogcatFileProvider.getAuthority(context),
                exportedFile
            )
            val shareIntent = settings.config.logFileShare.createIntent(
                context,
                logUri,
                exportedFile.name
            )
            val isSharingSupported =
                settings.config.logFileShare.launchIntent(context, shareIntent)
            if (!isSharingSupported) {
                showFeedback(
                    rootView,
                    R.string.logcat_viewer_not_support_on_this_device
                )
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

    private fun File.writeLogsSecurely(
        logs: Array<LogItem>,
        format: LogFileFormat
    ) {
        this.outputStream().bufferedWriter().use { writer ->
            format.writeLogs(this.name, logs, writer)
        }
    }
}
