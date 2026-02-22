package com.github.logviewer

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Using logcat to dump log entries to a file for a specific time period.
 *
 * This object can be reused for multiple dumps. It uses [GlobalScope].
 *
 * @param context the application context not the context of an activity
 * @param logFileName an implementation that might create logfile name from a timestamp
 * @param logFileFormat the formatting of the logfile
 * @param logStorageLocation where to store the log file
 *
 */
class LogcatDumper(
    context: Context,
    private val logFileName: LogFileNameFromTimestamp,
    logFileFormat: LogFileFormat = Settings.Default.config.logfileFormat,
    logStorageLocation: ExportLogFileUtils.StorageLocation =
        ExportLogFileUtils.StorageLocation.CACHE_EXTERNAL
) {

    private val settings = createInternalSettings(
        logFileName,
        logFileFormat,
        logStorageLocation
    )

    private val logcatReader = LogcatReader(settings)

    private val fileAdapter = LogcatDumpAdapter(context, settings)

    /**
     * dump logcat data to logfile using [kotlinx.coroutines.GlobalScope]
     *
     * @param timestamp the time when a crash happened since epoch in milliseconds
     * @param captureMsTimePeriodBeforeTimestamp we want only that time period of log
     *                                           entries before a crash happened (in milliseconds)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun dump(timestamp: Long, captureMsTimePeriodBeforeTimestamp: Long) {
        try {
            timestamp.also {
                logFileName.setTimestamp(it)
                fileAdapter.startCaptureLogItemsSinceTime = it - captureMsTimePeriodBeforeTimestamp
            }

            logcatReader.startReadLogcat(fileAdapter, emptyList(), GlobalScope)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * we need an independent settings object for dumping a logfile while an Exceptions happens
     */
    private fun createInternalSettings(
        logFileName: LogFileName,
        logFileFormat: LogFileFormat,
        logStorageLocation: ExportLogFileUtils.StorageLocation
    ): Settings {
        /** create independent instance of [Settings.Default] */
        val settings = Settings()
        settings.update { current ->
            current.copy(
                logfileFormat = logFileFormat,
                logFileName = logFileName,
                logOpMode = LogcatReader.OperationMode.DUMP,
                logStorageLocation = logStorageLocation
            )
        }
        return settings
    }

    /**
     * use [LogcatSink] to write the logfile.
     * @param context the application context not the context of an activity
     */
    private class LogcatDumpAdapter(
        private val context: Context,
        settings: Settings
    ) : LogcatSink {
        /**
         * Capture all [LogItem]'s starting at this time in ms since epoch.
         */
        var startCaptureLogItemsSinceTime: Long = 0
        private val exportLogFileUtils = ExportLogFileUtils(settings)
        private val allFilteredLogItemsForExportingToFile: ArrayList<LogItem> = ArrayList()

        override fun appendList(newItems: List<LogItem>) {
            val filteredItems =
                newItems.filter { it.time.getTime() >= startCaptureLogItemsSinceTime }
            allFilteredLogItemsForExportingToFile.addAll(filteredItems)
        }

        override fun onFinish() {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                exportLogFileUtils.writeLogDataToFile(
                    context,
                    allFilteredLogItemsForExportingToFile.toTypedArray()
                )
            }
        }
    }

    /**
     * Extend interface to set a timestamp as input to create a filename for the logfile.
     */
    interface LogFileNameFromTimestamp : LogFileName {
        /**
         * Timestamp in ms since epoch as input to create a filename in [LogFileName.getLogFileName].
         *
         * Of course, you do not have to use the timestamp if your requirements differ
         */
        fun setTimestamp(timestamp: Long)
    }
}