package com.group09.ComicReader.data.remote;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

final class ApiRequestMetrics {

    private static final long DEFAULT_WINDOW_MS = 10_000L;

    private final long windowMs;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    ApiRequestMetrics() {
        this(DEFAULT_WINDOW_MS);
    }

    ApiRequestMetrics(long windowMs) {
        this.windowMs = Math.max(1L, windowMs);
    }

    int increment(@NonNull String endpointKey) {
        return increment(endpointKey, System.currentTimeMillis());
    }

    int increment(@NonNull String endpointKey, long nowMs) {
        String safeKey = endpointKey.trim().isEmpty() ? "UNKNOWN" : endpointKey.trim();
        Counter counter = counters.computeIfAbsent(safeKey, key -> new Counter());
        return counter.increment(nowMs, windowMs);
    }

    private static final class Counter {
        private long windowStartMs = Long.MIN_VALUE;
        private int count = 0;

        synchronized int increment(long nowMs, long windowMs) {
            if (windowStartMs == Long.MIN_VALUE || nowMs - windowStartMs >= windowMs) {
                windowStartMs = nowMs;
                count = 1;
                return count;
            }
            count += 1;
            return count;
        }
    }
}
