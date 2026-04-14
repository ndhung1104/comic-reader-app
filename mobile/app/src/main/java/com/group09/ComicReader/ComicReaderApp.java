package com.group09.ComicReader;

import android.app.Application;

import androidx.annotation.NonNull;

import com.group09.ComicReader.util.PerfLogger;
import com.group09.ComicReader.util.PerfSession;

public class ComicReaderApp extends Application {
    private static final String SCREEN_NAME = "ComicReaderApp";

    @Override
    public void onCreate() {
        super.onCreate();
        logMemorySnapshot("app_create");
    }

    private void logMemorySnapshot(@NonNull String event) {
        PerfSession.MemorySnapshot snapshot = PerfSession.captureMemorySnapshot();
        PerfLogger.d(
                PerfLogger.TAG_APP,
                SCREEN_NAME,
                event,
                PerfLogger.kv("usedMb", snapshot.getUsedMb()),
                PerfLogger.kv("freeMb", snapshot.getFreeMb()),
                PerfLogger.kv("maxMb", snapshot.getMaxMb()),
                PerfLogger.kv("nativeHeapMb", snapshot.getNativeHeapMb()));
    }
}
