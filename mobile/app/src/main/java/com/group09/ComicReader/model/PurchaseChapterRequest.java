package com.group09.ComicReader.model;

public class PurchaseChapterRequest {
    private final Long chapterId;
    private final String currency;

    public PurchaseChapterRequest(Long chapterId, String currency) {
        this.chapterId = chapterId;
        this.currency = currency;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public String getCurrency() {
        return currency;
    }
}
