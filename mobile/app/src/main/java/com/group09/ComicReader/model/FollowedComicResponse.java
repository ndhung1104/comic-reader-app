package com.group09.ComicReader.model;

public class FollowedComicResponse {
    private Long comicId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer totalChapters;
    private Integer lastReadChapterNumber;
    private String lastReadChapterTitle;

    public LibraryItem toLibraryItem() {
        int safeComicId = comicId == null ? 0 : comicId.intValue();
        int safeTotalChapters = totalChapters == null ? 0 : totalChapters;
        String label = lastReadChapterNumber == null
                ? "Following this comic"
                : "Last read: Chapter " + lastReadChapterNumber;
        return new LibraryItem(
                safeComicId,
                title == null ? "" : title,
                author == null ? "Unknown" : author,
                coverUrl == null ? "" : coverUrl,
                safeTotalChapters,
                lastReadChapterNumber,
                null,
                null,
                label
        );
    }
}
