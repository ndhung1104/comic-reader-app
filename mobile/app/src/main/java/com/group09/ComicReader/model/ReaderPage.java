package com.group09.ComicReader.model;

public class ReaderPage {

    private final int pageNumber;
    private final String imageUrl;

    public ReaderPage(int pageNumber, String imageUrl) {
        this.pageNumber = pageNumber;
        this.imageUrl = imageUrl;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
