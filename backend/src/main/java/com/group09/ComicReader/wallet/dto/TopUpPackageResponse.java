package com.group09.ComicReader.wallet.dto;

public class TopUpPackageResponse {

    private Long id;
    private String name;
    private Integer coins;
    private String priceLabel;
    private String bonusLabel;
    private Boolean active;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCoins() { return coins; }
    public void setCoins(Integer coins) { this.coins = coins; }

    public String getPriceLabel() { return priceLabel; }
    public void setPriceLabel(String priceLabel) { this.priceLabel = priceLabel; }

    public String getBonusLabel() { return bonusLabel; }
    public void setBonusLabel(String bonusLabel) { this.bonusLabel = bonusLabel; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
