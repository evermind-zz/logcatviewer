package com.github.logviewer;


import android.content.Context;

import androidx.core.content.FileProvider;

public class LogcatFileProvider extends FileProvider {
    public static String getAuthority(final Context context) {
        return context.getPackageName() + BuildConfig.LOGCAT_TOOLKIT_AUTHORITY_POSTFIX;
    }
}
