package com.github.logviewer.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.github.logviewer.LogcatFileProvider

interface LogFileShare {
    /**
     *  Create the [Intent] to share it logfile.
     *
     * @param context the android context
     * @param logUri the Uri that the [LogcatFileProvider] provided for the to be shared logfile
     * @param exportedFileName the name of the logfile
     * @return the [Intent] to be feed via [launchIntent] to be shared there
     */
    fun createIntent(context: Context, logUri: Uri, exportedFileName: String): Intent

    /**
     * The launch place for an activity etc. to share the beforehand created Intent from [createIntent]
     *
     * @param context the android context
     * @param shareIntent the intent you created in [createIntent]
     * @return true if sharing is possible. False shows a toast/snackbar that it is impossible to share
     */
    fun launchIntent(context: Context, shareIntent: Intent): Boolean
}

/**
 * Default implementation that could be overwritten or extended the way it suits your app.
 */
open class LogFileShareDefault() : LogFileShare {
    override fun createIntent(
        context: Context,
        logUri: Uri,
        exportedFileName: String
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                "text/plain"
            } else {
                "application/octet-stream"
            }

            putExtra(Intent.EXTRA_STREAM, logUri)
            putExtra(Intent.EXTRA_TITLE, exportedFileName)
        }
    }

    override fun launchIntent(
        context: Context,
        shareIntent: Intent
    ): Boolean {
        if (context.packageManager.queryIntentActivities(shareIntent, 0).isEmpty()) {
            return false
        } else {
            val chooserIntent = Intent.createChooser(shareIntent, "Share logfile").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            return true
        }
    }
}
