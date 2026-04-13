package com.group09.ComicReader.ui.reader;

public final class ReaderProgressPositionResolver {

    private ReaderProgressPositionResolver() {
    }

    public static int resolvePagePosition(int firstVisibleAdapterPosition, int pageCount) {
        if (pageCount <= 0 || firstVisibleAdapterPosition < 0) {
            return -1;
        }
        return Math.max(0, Math.min(firstVisibleAdapterPosition, pageCount - 1));
    }
}
