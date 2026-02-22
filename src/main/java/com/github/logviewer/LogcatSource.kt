package com.github.logviewer

import android.os.Build
import de.brudaswen.android.logcat.core.data.LogcatItem

interface LogcatSource {

    fun getProcess(): Process
    fun shouldInclude(item: LogcatItem): Boolean
}

fun getLogcatSource(opMode: LogcatReader.OperationMode): LogcatSource {
    val processPid = android.os.Process.myPid()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ModernLogcatSource(processPid, opMode)
    } else {
        LegacyLogcatSource(processPid, opMode)
    }
}

class ModernLogcatSource(
    private val processPid: Int,
    private val opMode: LogcatReader.OperationMode
) : LogcatSource {
    override fun getProcess(): Process =
        if (opMode == LogcatReader.OperationMode.DUMP) {
            ProcessBuilder("logcat", "-d", "-B", "--pid=$processPid").start()
        } else {
            ProcessBuilder("logcat", "-B", "--pid=$processPid").start()
        }

    override fun shouldInclude(item: LogcatItem): Boolean = true
}

class LegacyLogcatSource(
    private val processPid: Int,
    private val opMode: LogcatReader.OperationMode
) : LogcatSource {
    override fun getProcess(): Process =
        if (opMode == LogcatReader.OperationMode.DUMP) {
            ProcessBuilder("logcat", "-d", "-B").start()
        } else {
            ProcessBuilder("logcat", "-B").start()
        }

    override fun shouldInclude(item: LogcatItem): Boolean {
        // for KitKat, we have to filter the PID manually as logcat does not
        // offer --pid
        return item.pid == processPid
    }
}
