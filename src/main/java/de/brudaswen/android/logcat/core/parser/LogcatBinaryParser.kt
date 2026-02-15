package de.brudaswen.android.logcat.core.parser

import de.brudaswen.android.logcat.core.data.LogcatItem
import de.brudaswen.android.logcat.core.data.LogcatLevel
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.readIntLe
import kotlinx.io.readString
import kotlinx.io.readUShortLe
import kotlinx.io.write
import java.io.Closeable
import java.io.InputStream

/**
 * Logcat parser that parses binary output of `adb logcat --binary`.
 *
 * @param input The input stream to parse.
 */
public class LogcatBinaryParser(
    private val input: InputStream,
) : Closeable by input {

    companion object {
        const val V1_HEADER_SIZE = 20

    }
    private val buffer = Buffer()

    /**
     * Parse one [LogcatItem] from the current [input] stream.
     *
     * @return The parsed [LogcatItem] or `null` if stream reached EOF.
     */
    fun parseItem(): LogcatItem?  {
        val firstByte = input.read()
        if (firstByte == -1) return null

        // Read v1 header
        buffer.writeByte(firstByte.toByte())
        buffer.write(input = input, byteCount = 19)

        val len = buffer.readUShortLe().toInt()
        var headerSize = buffer.readUShortLe().toInt()
        val pid = buffer.readIntLe()
        val tid = buffer.readIntLe()
        val sec = buffer.readIntLe()
        val nsec = buffer.readIntLe()
        var euid = -1 // v2/v3
        var lid = -1  // v3

        headerSize = if (headerSize < V1_HEADER_SIZE) {
            V1_HEADER_SIZE // V1 has no explicit headerSize field -> it's just padding and mostly zero)
        } else {
            headerSize
        }

        // Read additional header fields
        val additionalHeaderBytes = (headerSize - V1_HEADER_SIZE).coerceAtLeast(0).toLong()
        if (additionalHeaderBytes > 0) {
            buffer.write(input = input, byteCount = additionalHeaderBytes)

            if (additionalHeaderBytes >= 4) euid = buffer.readIntLe()
            if (additionalHeaderBytes >= 8) lid = buffer.readIntLe()

            // skip remaining if there is any
            if (additionalHeaderBytes > 8) {
                buffer.skip(additionalHeaderBytes - 8)
            }
        }


        // Read payload
        buffer.write(input = input, byteCount = len.toLong())

        val priority = buffer.readByte()

        val payload = buffer.readString()
        val texts = payload.split('\u0000', limit = 2)
        val tag = texts.getOrNull(0).orEmpty()
        val message = texts.getOrNull(1).orEmpty().removeSuffix("\u0000").trim()

        // Clear buffer
        buffer.clear()

        // Convert raw values to item
        return LogcatItem(
            sec = sec,
            nsec = nsec,
            priority = priority,
            pid = pid,
            tid = tid,
            tag = tag,
            message = message,
        )
    }

    private fun LogcatItem(
        sec: Int,
        nsec: Int,
        priority: Byte,
        pid: Int,
        tid: Int,
        tag: String,
        message: String,
    ): LogcatItem {
        val date = Instant.fromEpochSeconds(
            epochSeconds = sec.toLong(),
            nanosecondAdjustment = nsec,
        )

        val level = when (priority) {
            2.toByte() -> LogcatLevel.Verbose
            3.toByte() -> LogcatLevel.Debug
            4.toByte() -> LogcatLevel.Info
            5.toByte() -> LogcatLevel.Warning
            6.toByte() -> LogcatLevel.Error
            7.toByte() -> LogcatLevel.Fatal
            else -> null
        }

        return LogcatItem(
            date = date,
            pid = pid,
            tid = tid,
            level = level,
            tag = tag,
            message = message,
        )
    }
}
