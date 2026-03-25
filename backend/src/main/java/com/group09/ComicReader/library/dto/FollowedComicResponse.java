package com.group09.ComicReader.library.dto;

public class FollowedComicResponse {

    private Long comicId;
    private String title;
    private String author;
    private String coverUrl;
    private Integer totalChapters;
    private Integer lastReadChapterNumber;
    private String lastReadChapterTitle;

    public Long getComicId() {
        return comicId;
    }

    public void setComicId(Long comicId) {
        this.comicId = comicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(Integer totalChapters) {
        this.totalChapters = totalChapters;
    }

    public Integer getLastReadChapterNumber() {
        return lastReadChapterNumber;
    }

    public void setLastReadChapterNumber(Integer lastReadChapterNumber) {
        this.lastReadChapterNumber = lastReadChapterNumber;
    }

    public String getLastReadChapterTitle() {
        return lastReadChapterTitle;
    }

    public void setLastReadChapterTitle(String lastReadChapterTitle) {
        this.lastReadChapterTitle = lastReadChapterTitle;
    }
}
