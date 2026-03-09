package com.group09.ComicReader.model;

public class ReaderPageResponse {

    private Long id;
    private Long chapterId;
    private Integer pageNumber;
    private String imageUrl;

    public Long getId() {
        return id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
