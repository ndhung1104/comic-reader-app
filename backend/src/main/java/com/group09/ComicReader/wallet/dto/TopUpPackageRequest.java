package com.group09.ComicReader.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TopUpPackageRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Coins is required")
    @Min(value = 1, message = "Coins must be at least 1")
    private Integer coins;

    @NotBlank(message = "Price label is required")
    private String priceLabel;

    private String bonusLabel = "";

    @Min(value = 0)
    private Integer sortOrder = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCoins() { return coins; }
    public void setCoins(Integer coins) { this.coins = coins; }

    public String getPriceLabel() { return priceLabel; }
    public void setPriceLabel(String priceLabel) { this.priceLabel = priceLabel; }

    public String getBonusLabel() { return bonusLabel; }
    public void setBonusLabel(String bonusLabel) { this.bonusLabel = bonusLabel; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
