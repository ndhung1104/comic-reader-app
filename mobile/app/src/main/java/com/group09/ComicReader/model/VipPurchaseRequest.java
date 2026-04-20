package com.group09.ComicReader.model;

public class VipPurchaseRequest {
    private final String plan;
    private final String currency;

    public VipPurchaseRequest(String plan, String currency) {
        this.plan = plan;
        this.currency = currency;
    }

    public String getPlan() {
        return plan;
    }

    public String getCurrency() {
        return currency;
    }
}
