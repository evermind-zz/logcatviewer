package com.github.logviewer

import android.os.Build
import de.brudaswen.android.logcat.core.data.LogcatItem

interface LogcatSource {
    fun getProcess(): Process
    fun shouldInclude(item: LogcatItem): Boolean
}

fun getLogcatSource(processPid: Int): LogcatSource =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ModernLogcatSource(processPid)
    } else {
        LegacyLogcatSource(processPid)
    }

class ModernLogcatSource(private val processPid: Int) : LogcatSource {
    override fun getProcess(): Process =
        ProcessBuilder("logcat", "-B", "--pid=$processPid").start()

    override fun shouldInclude(item: LogcatItem): Boolean = true
}

class LegacyLogcatSource(private val processPid: Int) : LogcatSource {
    override fun getProcess(): Process =
        ProcessBuilder("logcat", "-B").start()

    override fun shouldInclude(item: LogcatItem): Boolean {
        // for KitKat we have to filter the PID manually as logcat does not
        // offer --pid
        return item.pid == processPid
    }
}
