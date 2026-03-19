package com.group09.ComicReader.model;

public class ReaderPage {

    private final int pageNumber;
    private final String imageUrl;
    private final int imageWidth;
    private final int imageHeight;

    public ReaderPage(int pageNumber, String imageUrl) {
        this(pageNumber, imageUrl, 0, 0);
    }

    public ReaderPage(int pageNumber, String imageUrl, int imageWidth, int imageHeight) {
        this.pageNumber = pageNumber;
        this.imageUrl = imageUrl;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }
}
