package com.github.logviewer

import com.github.logviewer.settings.CleanupConfig
import com.github.logviewer.settings.DeleteAllExceptLastStrategy
import com.github.logviewer.settings.LogFileShare
import com.github.logviewer.settings.LogFileShareDefault
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * global settings object.
 */
object Settings {
    private val configRef = AtomicReference(LogConfig())

    val config: LogConfig get() = configRef.get()

    /**
     * This method takes the current state and allows to change only fields you want to change.
     */
    fun update(transform: (LogConfig) -> LogConfig) {
        var success = false
        while (!success) {
            val current = configRef.get()
            val next = transform(current)
            success = configRef.compareAndSet(current, next)
        }
    }
}

data class LogConfig(
    var logfileFormat: LogFileFormat = object : LogFileFormat {
        override suspend fun writeLogs(
            logFileName: String,
            logs: Array<LogItem>,
            writer: BufferedWriter
        ) {
            for (log in logs) {
                writer.write(log.origin + "\n")
            }
        }
    },

    val logFilePrefix: LogFilePrefix = object : LogFilePrefix {
        override suspend fun getPrefix(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            return dateFormat.format(Date())
        }
    },

    val logFileShare: LogFileShare = LogFileShareDefault(),
    /**
     *  chosen default strategy delete all except last 10 logs
     */
    var logCleanupStrategy: CleanupConfig = CleanupConfig(DeleteAllExceptLastStrategy(), 10)
)

/**
 * set your implementation if you want to change the output format of the logfile.
 */
interface LogFileFormat {
    /**
     * write logs to output.
     */
    suspend fun writeLogs(logFileName: String, logs: Array<LogItem>, writer: BufferedWriter)
}

/**
 * Set your implementation if you want to override the config filename prefix.
 *
 * Eg your prefix is my_logfile. the final name will be my_logfile.log
 */
interface LogFilePrefix {
    /**
     * write logs to output.
     */
    suspend fun getPrefix(): String
}
