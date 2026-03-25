package com.group09.ComicReader.model;

public class TopUpRequest {
    private final Integer amount;
    private final String currency;
    private final String referenceId;

    public TopUpRequest(Integer amount, String currency, String referenceId) {
        this.amount = amount;
        this.currency = currency;
        this.referenceId = referenceId;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
