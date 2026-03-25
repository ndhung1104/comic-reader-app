package com.group09.ComicReader.model;

public class ReadingHistoryRequest {
    private final Long comicId;
    private final Long chapterId;
    private final Integer pageNumber;

    public ReadingHistoryRequest(Long comicId, Long chapterId, Integer pageNumber) {
        this.comicId = comicId;
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
    }

    public Long getComicId() {
        return comicId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }
}
