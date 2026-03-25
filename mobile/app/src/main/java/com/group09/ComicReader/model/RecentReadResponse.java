package com.group09.ComicReader.model;

public class RecentReadResponse {
    private Long comicId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer totalChapters;
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer pageNumber;
    private String lastReadAt;

    public LibraryItem toLibraryItem() {
        int safeComicId = comicId == null ? 0 : comicId.intValue();
        int safeTotalChapters = totalChapters == null ? 0 : totalChapters;
        Integer safeChapterId = chapterId == null ? null : chapterId.intValue();
        String label = chapterNumber == null
                ? "Continue reading"
                : "Continue chapter " + chapterNumber;
        return new LibraryItem(
                safeComicId,
                title == null ? "" : title,
                author == null ? "Unknown" : author,
                coverUrl == null ? "" : coverUrl,
                safeTotalChapters,
                chapterNumber,
                safeChapterId,
                chapterNumber,
                label
        );
    }
}
