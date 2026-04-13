package com.group09.ComicReader.data.download;

import com.group09.ComicReader.data.local.download.ChapterDownloadEntity;
import com.group09.ComicReader.data.local.download.ChapterPageFileEntity;
import com.group09.ComicReader.model.ReaderPage;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReaderContentSourceResolverTest {

    @Test
    public void resolveOfflinePages_shouldReturnEmpty_whenDownloadNotCompleted() {
        ReaderContentSourceResolver resolver = new ReaderContentSourceResolver();
        ChapterDownloadEntity download = new ChapterDownloadEntity(1L, DownloadStatus.DOWNLOADING.name(), 20, null, 0L);

        List<ReaderPage> pages = resolver.resolveOfflinePages(download, Collections.emptyList());

        assertTrue(pages.isEmpty());
    }

    @Test
    public void resolveOfflinePages_shouldReturnExistingFiles_whenCompleted() throws Exception {
        ReaderContentSourceResolver resolver = new ReaderContentSourceResolver();
        ChapterDownloadEntity download = new ChapterDownloadEntity(2L, DownloadStatus.COMPLETED.name(), 100, null, 0L);

        File tempFile = File.createTempFile("reader-page", ".jpg");
        tempFile.deleteOnExit();

        ChapterPageFileEntity valid = new ChapterPageFileEntity(2L, 3, "https://example.com/a.jpg",
                tempFile.getAbsolutePath(), "hash");
        ChapterPageFileEntity missing = new ChapterPageFileEntity(2L, 4, "https://example.com/b.jpg",
                tempFile.getAbsolutePath() + ".missing", "hash2");

        List<ReaderPage> pages = resolver.resolveOfflinePages(download, Arrays.asList(valid, missing));

        assertEquals(1, pages.size());
        assertEquals(3, pages.get(0).getPageNumber());
        assertTrue(pages.get(0).getImageUrl().startsWith("file:"));
    }
}
