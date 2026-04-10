package com.group09.ComicReader.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PurchaseChapterRequest {

    @NotNull(message = "chapterId is required")
    private Long chapterId;

    @Pattern(regexp = "^(?i)(COIN|POINT)$", message = "currency must be COIN or POINT")
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
