package com.group09.ComicReader.wallet.dto;

import jakarta.validation.constraints.NotNull;

public class PurchaseChapterRequest {

    @NotNull
    private Long chapterId;

    private String currency; // COIN or POINT, defaults to COIN

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
