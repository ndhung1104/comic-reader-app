package com.group09.ComicReader.ui.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderProgressPositionResolverTest {

    @Test
    public void resolvePagePosition_shouldReturnMinusOne_forInvalidInputs() {
        assertEquals(-1, ReaderProgressPositionResolver.resolvePagePosition(-1, 5));
        assertEquals(-1, ReaderProgressPositionResolver.resolvePagePosition(0, 0));
    }

    @Test
    public void resolvePagePosition_shouldKeepPageIndex_whenInRange() {
        assertEquals(2, ReaderProgressPositionResolver.resolvePagePosition(2, 5));
    }

    @Test
    public void resolvePagePosition_shouldClampToLastPage_whenFooterIsVisible() {
        assertEquals(4, ReaderProgressPositionResolver.resolvePagePosition(5, 5));
    }
}
