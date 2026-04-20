package com.group09.ComicReader.share;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShareContentTest {

    @Test
    public void buildShareTextShouldIncludeChapterCaptionAndLink() {
        ShareContent content = new ShareContent(
                ShareContent.TYPE_CHAPTER,
                "Solo Leveling",
                "https://comicreader.app/share/chapter/12",
                12
        );

        String shareText = content.buildShareText(
                "Read %1$s on ComicReader",
                "Read %1$s - Chapter %2$d on ComicReader",
                "Read on ComicReader"
        );

        assertTrue(shareText.startsWith("Read Solo Leveling - Chapter 12 on ComicReader"));
        assertTrue(shareText.endsWith("https://comicreader.app/share/chapter/12"));
    }

    @Test
    public void buildCaptionShouldFallBackWhenChapterNumberMissing() {
        ShareContent content = new ShareContent(
                ShareContent.TYPE_CHAPTER,
                "Solo Leveling",
                "https://comicreader.app/share/chapter/12",
                0
        );

        String caption = content.buildCaption(
                "Read %1$s on ComicReader",
                "Read %1$s - Chapter %2$d on ComicReader",
                "Read on ComicReader"
        );

        assertEquals("Read on ComicReader", caption);
    }
}
