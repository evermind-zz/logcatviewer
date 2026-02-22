package com.github.logviewer

import de.brudaswen.android.logcat.core.data.LogcatItem
import de.brudaswen.android.logcat.core.parser.LogcatBinaryParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LogcatReader(val settings: Settings = Settings.Default) {

    private var logcatJob: Job? = null

    /**
     * we collect the entries and send them to the adapter every [timeMillis] ms or when no of [size] have been collected.
     */
    private fun <T> Flow<T>.chunked(size: Int, timeMillis: Long): Flow<List<T>> = flow {
        val buffer = mutableListOf<T>()
        var lastEmitTime = System.currentTimeMillis()

        collect { value ->
            buffer.add(value)
            val currentTime = System.currentTimeMillis()

            // Send when buffer is full or time has expired
            if (buffer.size >= size || (currentTime - lastEmitTime >= timeMillis)) {
                emit(buffer.toList())
                buffer.clear()
                lastEmitTime = currentTime
            }
        }
        // Send remaining, if any
        if (buffer.isNotEmpty()) emit(buffer)
    }

    private fun getLogcatFlow(excludeList: List<Pattern>) = flow {
        val processPid = android.os.Process.myPid()
        val logcatSource = getLogcatSource(processPid)
        val process = logcatSource.getProcess()

        try {
            process.inputStream.buffered().use { inputStream ->
                LogcatBinaryParser(inputStream).use { parser ->
                    while (currentCoroutineContext().isActive) {
                        val logcatItem = parser.parseItem() ?: break
                        if (!logcatSource.shouldInclude(logcatItem)) continue

                        val item = LogItem(logcatItem)

                        if (excludeList.none { it.matcher(item.origin).matches() }) {
                            emit(item)
                        }
                    }
                }
            }
        } finally {
            // terminate the process also when the flow is stopped
            process.destroy()
        }
    }.flowOn(Dispatchers.IO)


    /**
     * starts the logcat reader process.
     *
     * @param logcatSink where to push the collected [LogItem]
     * @param excludeList exclude some pattern
     * @param coroutineScope if [logcatSink] is working within the UI, the scope should run on the UI-Thread
     *
     */
    fun startReadLogcat(
        logcatSink: LogcatSink,
        excludeList: List<Pattern>,
        coroutineScope: CoroutineScope
    ) {
        // If an old job is still running, stop it to be on the safe side.
        stopReadLogcat()

        logcatJob = coroutineScope.launch {
            getLogcatFlow(excludeList)
                .chunked(size = 50, timeMillis = 100L)
                .collect { items ->
                    logcatSink.appendList(items)
                }
        } .apply {
            invokeOnCompletion {
                logcatSink.onFinish()
            }
        }
    }

    fun stopReadLogcat() {
        // Breaks the coroutine and thus the flow & process
        logcatJob?.cancel()
        logcatJob = null
    }

    companion object {
        const val EXCLUDE_LIST_KEY = "exclude_list"
    }
}
