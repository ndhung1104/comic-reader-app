package com.group09.ComicReader.model;

public class LibraryItem {
    private final int comicId;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final int totalChapters;
    private final Integer progressChapter;
    private final Integer resumeChapterId;
    private final Integer resumeChapterNumber;
    private final String progressLabel;

    public LibraryItem(int comicId, String title, String author, String coverUrl, int totalChapters,
                       Integer progressChapter, Integer resumeChapterId, Integer resumeChapterNumber,
                       String progressLabel) {
        this.comicId = comicId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.totalChapters = totalChapters;
        this.progressChapter = progressChapter;
        this.resumeChapterId = resumeChapterId;
        this.resumeChapterNumber = resumeChapterNumber;
        this.progressLabel = progressLabel;
    }

    public int getComicId() {
        return comicId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public Integer getProgressChapter() {
        return progressChapter;
    }

    public Integer getResumeChapterId() {
        return resumeChapterId;
    }

    public Integer getResumeChapterNumber() {
        return resumeChapterNumber;
    }

    public String getProgressLabel() {
        return progressLabel;
    }
}
