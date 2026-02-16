package com.github.logviewer.settings

import com.github.logviewer.settings.LogFileDeleteStrategy.Companion.USE_DEFAULT_THRESHOLD
import java.io.File

/**
 * Strategy interface for managing the cleanup of generated log files.
 *
 * This interface allows library consumers to define custom logic for deleting
 * old log exports to prevent storage bloat, which is especially critical on
 * legacy devices with limited disk space.
 */
interface LogFileDeleteStrategy {
    companion object {
        const val USE_DEFAULT_THRESHOLD = 0

    }

    /**
     * Applies the deletion logic to the specified directory.
     *
     * @param logDir    The directory where log files are stored.
     * @param threshold A versatile integer parameter used to define the deletion limit.
     *                  Depending on the implementation, this value can represent:
     *                  - A maximum number of files to keep (e.g., N newest files).
     *                  - A time-based limit in minutes (e.g., older than N minutes).
     *                  - A default fallback value of [USE_DEFAULT_THRESHOLD] if no specific threshold is provided.
     */
    fun apply(logDir: File, threshold: Int = USE_DEFAULT_THRESHOLD)
}

class CleanupConfig(
    val strategy: LogFileDeleteStrategy,
    val threshold: Int = USE_DEFAULT_THRESHOLD
) {
    fun apply(logDir: File) {
        strategy.apply(logDir, threshold)
    }
}

open class DeleteAllExceptLastStrategy : LogFileDeleteStrategy {
    override fun apply(logDir: File, threshold: Int) {
        val files = logDir.listFiles { f -> f.extension == "log" } ?: return
        if (files.size <= 1) return

        // sort by time (newest first) and start deleting with index 1
        files.sortedByDescending { it.lastModified() }
            .drop(1)
            .forEach { it.delete() }
    }
}

open class KeepLastNFilesStrategy : LogFileDeleteStrategy {
    override fun apply(logDir: File, threshold: Int) {
        val defaultKeepNoOfFilesThreshold = 5
        val limit =
            if (threshold <= USE_DEFAULT_THRESHOLD) defaultKeepNoOfFilesThreshold else threshold
        val files = logDir.listFiles { f -> f.extension == "log" } ?: return

        files.sortedByDescending { it.lastModified() }
            .drop(limit)
            .forEach { it.delete() }
    }
}

open class DeleteOlderThanNMinutesStrategy : LogFileDeleteStrategy {
    override fun apply(logDir: File, threshold: Int) {
        val defaultKeepOlderThanMinutes = 60
        val minutes = if (threshold <= 0) defaultKeepOlderThanMinutes else threshold
        val cutoff = System.currentTimeMillis() - (minutes * 60 * 1000L)

        logDir.listFiles { f -> f.extension == "log" }?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
