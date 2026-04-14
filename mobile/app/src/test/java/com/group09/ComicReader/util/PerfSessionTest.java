package com.group09.ComicReader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PerfSessionTest {

    @Test
    public void sessionId_isStableAndNonEmpty() {
        String first = PerfSession.getSessionId();
        String second = PerfSession.getSessionId();

        assertNotNull(first);
        assertFalse(first.trim().isEmpty());
        assertEquals(first, second);
    }

    @Test
    public void memorySnapshot_containsExpectedBounds() {
        PerfSession.MemorySnapshot snapshot = PerfSession.captureMemorySnapshot();

        assertTrue(snapshot.getMaxMb() > 0L);
        assertTrue(snapshot.getUsedMb() >= 0L);
        assertTrue(snapshot.getFreeMb() >= 0L);
        assertTrue(snapshot.getNativeHeapMb() >= 0L);
        assertTrue(snapshot.getUsedMb() <= snapshot.getMaxMb());
    }
}
