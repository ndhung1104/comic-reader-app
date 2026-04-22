package com.group09.ComicReader.share;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShareUrlValidatorTest {

    @Test
    public void needsLocalWarningShouldFlagEmulatorHosts() {
        assertTrue(ShareUrlValidator.needsLocalWarning("http://10.0.2.2:8080/share/comic/1"));
        assertTrue(ShareUrlValidator.needsLocalWarning("http://localhost:8080/share/comic/1"));
    }

    @Test
    public void needsLocalWarningShouldIgnoreReachableHosts() {
        assertFalse(ShareUrlValidator.needsLocalWarning("https://comicreader.app/share/comic/1"));
        assertFalse(ShareUrlValidator.needsLocalWarning("http://192.168.1.10:8080/share/comic/1"));
    }
}
