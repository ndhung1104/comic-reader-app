package com.group09.ComicReader.model;

public class TransactionResponse {
    private Long id;
    private String type;
    private Integer amount;
    private String currency;
    private Integer balanceAfter;
    private String description;
    private String referenceId;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
