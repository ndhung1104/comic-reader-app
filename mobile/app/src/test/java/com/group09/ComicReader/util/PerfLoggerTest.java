package com.group09.ComicReader.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PerfLoggerTest {

    @Test
    public void format_includesCoreFieldsAndCustomPairs() {
        String message = PerfLogger.format(
                "ReaderActivity",
                "chapter_load_started",
                PerfLogger.kv("chapterId", 101),
                PerfLogger.kv("source", "deep_link"));

        assertTrue(message.contains("sessionId=" + PerfSession.getSessionId()));
        assertTrue(message.contains("screen=ReaderActivity"));
        assertTrue(message.contains("event=chapter_load_started"));
        assertTrue(message.contains("thread="));
        assertTrue(message.contains("chapterId=101"));
        assertTrue(message.contains("source=deep_link"));
    }

    @Test
    public void endTimer_appendsDuration() {
        String message = PerfLogger.endTimer(
                "ReaderActivity",
                "chapter_load_completed",
                PerfLogger.startTimer(),
                PerfLogger.kv("chapterId", 101));

        assertTrue(message.contains("event=chapter_load_completed"));
        assertTrue(message.contains("durationMs="));
        assertTrue(message.contains("chapterId=101"));
    }
}
