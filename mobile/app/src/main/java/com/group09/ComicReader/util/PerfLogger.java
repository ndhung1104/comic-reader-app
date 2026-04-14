package com.group09.ComicReader.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.group09.ComicReader.BuildConfig;

public final class PerfLogger {

    public static final String TAG_APP = "PERF_APP";
    public static final String TAG_NAV = "PERF_NAV";
    public static final String TAG_NET = "PERF_NET";
    public static final String TAG_READER = "PERF_READER";
    public static final String TAG_MEM = "PERF_MEM";
    public static final String TAG_STATE = "PERF_STATE";
    public static final String TAG_DL = "PERF_DL";

    private PerfLogger() {
    }

    public static boolean isDetailedLoggingEnabled() {
        return BuildConfig.DEBUG;
    }

    public static long startTimer() {
        return PerfSession.startTimer();
    }

    @NonNull
    public static String endTimer(
            @NonNull String screen,
            @NonNull String event,
            long startNs,
            @NonNull KeyValue... extras) {
        return format(screen, event, appendDuration(PerfSession.durationMs(startNs), extras));
    }

    @NonNull
    public static String format(
            @NonNull String screen,
            @NonNull String event,
            @NonNull KeyValue... extras) {
        StringBuilder builder = new StringBuilder();
        builder.append("sessionId=").append(PerfSession.getSessionId())
                .append(" screen=").append(screen)
                .append(" event=").append(event)
                .append(" thread=").append(PerfSession.currentThreadName());
        if (extras.length > 0) {
            for (KeyValue extra : extras) {
                if (extra == null) {
                    continue;
                }
                builder.append(' ')
                        .append(extra.key)
                        .append('=')
                        .append(extra.value);
            }
        }
        return builder.toString();
    }

    public static void d(
            @NonNull String tag,
            @NonNull String screen,
            @NonNull String event,
            @NonNull KeyValue... extras) {
        if (!isDetailedLoggingEnabled()) {
            return;
        }
        try {
            Log.d(tag, format(screen, event, extras));
        } catch (Throwable ignored) {
        }
    }

    public static void i(
            @NonNull String tag,
            @NonNull String screen,
            @NonNull String event,
            @NonNull KeyValue... extras) {
        try {
            Log.i(tag, format(screen, event, extras));
        } catch (Throwable ignored) {
        }
    }

    public static void w(
            @NonNull String tag,
            @NonNull String screen,
            @NonNull String event,
            @NonNull KeyValue... extras) {
        try {
            Log.w(tag, format(screen, event, extras));
        } catch (Throwable ignored) {
        }
    }

    public static void e(
            @NonNull String tag,
            @NonNull String screen,
            @NonNull String event,
            @NonNull Throwable throwable,
            @NonNull KeyValue... extras) {
        try {
            Log.e(tag, format(screen, event, extras), throwable);
        } catch (Throwable ignored) {
        }
    }

    @NonNull
    public static KeyValue kv(@NonNull String key, @NonNull Object value) {
        return new KeyValue(key, String.valueOf(value));
    }

    @NonNull
    private static KeyValue[] appendDuration(long durationMs, @NonNull KeyValue... extras) {
        KeyValue[] all = new KeyValue[extras.length + 1];
        all[0] = kv("durationMs", durationMs);
        System.arraycopy(extras, 0, all, 1, extras.length);
        return all;
    }

    public static final class KeyValue {
        private final String key;
        private final String value;

        private KeyValue(String key, String value) {
            this.key = sanitize(key);
            this.value = sanitize(value);
        }

        private static String sanitize(String raw) {
            if (raw == null) {
                return "null";
            }
            return raw.replace(' ', '_');
        }
    }
}
