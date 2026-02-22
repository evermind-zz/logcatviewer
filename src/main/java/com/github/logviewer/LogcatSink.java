package com.github.logviewer;

import java.util.List;

/**
 * push the logcat data to the consumer
 */
public interface LogcatSink {
    /**
     * Append chunk of [LogItem] data.
     *
     * @param newItems a chunk of logcat entries
     */
    void appendList(List<LogItem> newItems);

    /**
     * If all logcat entries are send
     */
    default void onFinish() {}
}
