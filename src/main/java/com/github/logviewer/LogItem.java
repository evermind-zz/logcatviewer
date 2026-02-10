package com.github.logviewer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorRes;
import de.brudaswen.android.logcat.core.data.LogcatItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LogItem implements Parcelable {

    private static final String PRIORITY_VERBOSE = "V";
    private static final String PRIORITY_DEBUG = "D";
    private static final String PRIORITY_INFO = "I";
    private static final String PRIORITY_WARNING = "W";
    private static final String PRIORITY_ERROR = "E";
    private static final String PRIORITY_FATAL = "F";

    private static final HashMap<String, Integer> LOGCAT_COLORS = new HashMap<String, Integer>() {{
        put(PRIORITY_VERBOSE, R.color.logcat_verbose);
        put(PRIORITY_DEBUG, R.color.logcat_debug);
        put(PRIORITY_INFO, R.color.logcat_info);
        put(PRIORITY_WARNING, R.color.logcat_warning);
        put(PRIORITY_ERROR, R.color.logcat_error);
        put(PRIORITY_FATAL, R.color.logcat_fatal);
    }};

    private static final ArrayList<String> SUPPORTED_FILTERS = new ArrayList<String>() {{
        add(PRIORITY_VERBOSE);
        add(PRIORITY_DEBUG);
        add(PRIORITY_INFO);
        add(PRIORITY_WARNING);
        add(PRIORITY_ERROR);
        add(PRIORITY_FATAL);
    }};

    public static final SimpleDateFormat LOGCAT_DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT);

    public Date time;
    public int processId;
    public int threadId;
    public String level;
    public String tag;
    public String content;
    public String origin;

    LogItem(LogcatItem logcatItem) throws IllegalStateException, ParseException {

        time = new Date(logcatItem.getDate().toEpochMilliseconds());

        processId = logcatItem.getPid();
        threadId = logcatItem.getTid();
        level = logcatItem.getLevel().name().substring(0, 1);
        tag = logcatItem.getTag();
        content = logcatItem.getMessage();
        origin = (String.format(Locale.ROOT,"%s %5d %5d %s TAG='%s' %s",
                LOGCAT_DATE_FORMAT.format(time), processId, threadId, level, tag, content));
    }

    @ColorRes
    int getColorRes() {
        return LOGCAT_COLORS.get(level);
    }

    boolean isFiltered(String filter) {
        return SUPPORTED_FILTERS.indexOf(level) < SUPPORTED_FILTERS.indexOf(filter);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.time != null ? this.time.getTime() : -1);
        dest.writeInt(this.processId);
        dest.writeInt(this.threadId);
        dest.writeString(this.level);
        dest.writeString(this.tag);
        dest.writeString(this.content);
        dest.writeString(this.origin);
    }

    private LogItem(Parcel in) {
        long tmpTime = in.readLong();
        this.time = tmpTime == -1 ? null : new Date(tmpTime);
        this.processId = in.readInt();
        this.threadId = in.readInt();
        this.level = in.readString();
        this.tag = in.readString();
        this.content = in.readString();
        this.origin = in.readString();
    }

    public static final Parcelable.Creator<LogItem> CREATOR = new Parcelable.Creator<LogItem>() {
        @Override
        public LogItem createFromParcel(Parcel source) {
            return new LogItem(source);
        }

        @Override
        public LogItem[] newArray(int size) {
            return new LogItem[size];
        }
    };
}
