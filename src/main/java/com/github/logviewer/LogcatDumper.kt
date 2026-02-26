package com.github.logviewer

import android.content.Context
import android.util.Log
import com.github.logviewer.settings.CleanupConfig
import com.github.logviewer.settings.KeepLastNFilesStrategy
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Using logcat to dump log entries to a file for a specific time period.
 *
 * - This object can be reused for multiple dumps.
 * - It uses [GlobalScope].
 * - It dumps only if no other dump is already ongoing
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
    logCleanupStrategy: CleanupConfig = CleanupConfig(KeepLastNFilesStrategy(), 2),
    logStorageLocation: ExportLogFileUtils.StorageLocation =
        ExportLogFileUtils.StorageLocation.CACHE_EXTERNAL
) {

    private val settings = createInternalSettings(
        logFileName,
        logFileFormat,
        logCleanupStrategy,
        logStorageLocation
    )

    /** only allow one [dump] to run at a time */
    private val isCurrentlyDumping = AtomicBoolean(false)

    private val logcatReader = LogcatReader(settings)

    private val fileAdapter = LogcatDumpAdapter(context, isCurrentlyDumping, settings)

    /**
     * expose the relative logFolder as internal Settings object is not visible to user.
     */
    fun getLogFolder(context: Context): File? {
        return settings.config.getLogFolder(context)
    }

    /**
     * dump logcat data to logfile using [kotlinx.coroutines.GlobalScope]
     *
     * @param timestamp the time when a crash happened since epoch in milliseconds
     * @param captureMsTimePeriodBeforeTimestamp we want only that time period of log
     *                                           entries before a crash happened (in milliseconds)
     * @param delayDump wait millis before starting to dump
     * @return false if logcat dumping is already ongoing. true if dumping started
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun dump(
        timestamp: Long,
        captureMsTimePeriodBeforeTimestamp: Long,
        delayDump: Long = 0
    ): Boolean {
        if (!isCurrentlyDumping.compareAndSet(false, true)) {
            Log.w(javaClass.simpleName, "dump() already active, ignore call.")
            return false
        }

        try {
            timestamp.also {
                logFileName.setTimestamp(it)
                fileAdapter.startCaptureLogItemsSinceTime = it - captureMsTimePeriodBeforeTimestamp
            }

            logcatReader.startReadLogcat(fileAdapter, emptyList(), GlobalScope, delayDump)

        } catch (e: Exception) {
            isCurrentlyDumping.set(false)
            e.printStackTrace()
        }
        return true
    }

    /**
     * we need an independent settings object for dumping a logfile while an Exceptions happens
     */
    private fun createInternalSettings(
        logFileName: LogFileName,
        logFileFormat: LogFileFormat,
        logCleanupStrategy: CleanupConfig,
        logStorageLocation: ExportLogFileUtils.StorageLocation
    ): Settings {
        /** create independent instance of [Settings.Default] */
        val settings = Settings()
        settings.update { current ->
            current.copy(
                logfileFormat = logFileFormat,
                logFileName = logFileName,
                logCleanupStrategy = logCleanupStrategy,
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
        private val isCurrentlyDumping: AtomicBoolean,
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
            /** as [LogcatDumper] uses [GlobalScope] we do not need to launch it in another scope */
            runBlocking {
                try {
                    exportLogFileUtils.writeLogDataToFile(
                        context,
                        allFilteredLogItemsForExportingToFile.toTypedArray()
                    )
                } finally {
                    // remove all entries for next round
                    allFilteredLogItemsForExportingToFile.clear()
                    isCurrentlyDumping.set(false)
                }
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