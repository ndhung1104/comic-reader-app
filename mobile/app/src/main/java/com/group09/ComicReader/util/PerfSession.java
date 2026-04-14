package com.group09.ComicReader.util;

import android.os.Debug;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.UUID;

public final class PerfSession {

    private static final long MB_DIVISOR = 1024L * 1024L;
    private static final String SESSION_ID = createSessionId();

    private PerfSession() {
    }

    @NonNull
    public static String getSessionId() {
        return SESSION_ID;
    }

    public static long startTimer() {
        return System.nanoTime();
    }

    public static long durationMs(long startNs) {
        if (startNs <= 0L) {
            return 0L;
        }
        long durationNs = System.nanoTime() - startNs;
        if (durationNs <= 0L) {
            return 0L;
        }
        return durationNs / 1_000_000L;
    }

    @NonNull
    public static String currentThreadName() {
        return Thread.currentThread().getName();
    }

    @NonNull
    public static MemorySnapshot captureMemorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = Math.max(0L, totalMemory - freeMemory);
        long nativeHeap = 0L;
        try {
            nativeHeap = Debug.getNativeHeapAllocatedSize();
        } catch (Throwable ignored) {
            nativeHeap = 0L;
        }
        return new MemorySnapshot(
                toMb(usedMemory),
                toMb(freeMemory),
                toMb(maxMemory),
                toMb(nativeHeap)
        );
    }

    private static long toMb(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return bytes / MB_DIVISOR;
    }

    @NonNull
    private static String createSessionId() {
        String random = UUID.randomUUID().toString().replace("-", "");
        if (random.length() > 8) {
            random = random.substring(0, 8);
        }
        return String.format(Locale.US, "%s-%d", random, System.currentTimeMillis());
    }

    public static final class MemorySnapshot {
        private final long usedMb;
        private final long freeMb;
        private final long maxMb;
        private final long nativeHeapMb;

        public MemorySnapshot(long usedMb, long freeMb, long maxMb, long nativeHeapMb) {
            this.usedMb = usedMb;
            this.freeMb = freeMb;
            this.maxMb = maxMb;
            this.nativeHeapMb = nativeHeapMb;
        }

        public long getUsedMb() {
            return usedMb;
        }

        public long getFreeMb() {
            return freeMb;
        }

        public long getMaxMb() {
            return maxMb;
        }

        public long getNativeHeapMb() {
            return nativeHeapMb;
        }
    }
}
