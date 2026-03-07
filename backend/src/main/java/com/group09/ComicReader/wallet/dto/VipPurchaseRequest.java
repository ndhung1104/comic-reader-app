package com.group09.ComicReader.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VipPurchaseRequest {

    @NotBlank
    private String plan; // MONTHLY, YEARLY

    private String currency; // COIN or POINT, defaults to COIN

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
