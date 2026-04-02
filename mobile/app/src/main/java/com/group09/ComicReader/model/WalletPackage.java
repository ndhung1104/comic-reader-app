package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;

public class WalletPackage {
    private Long id;
    private String name;
    private int coins;
    
    @SerializedName("priceLabel")
    private String price;
    
    @SerializedName("bonusLabel")
    private String bonus;
    
    private Boolean active;
    private Integer sortOrder;

    public WalletPackage() {}

    public WalletPackage(int coins, String price, String bonus) {
        this.coins = coins;
        this.price = price;
        this.bonus = bonus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getBonus() { return bonus; }
    public void setBonus(String bonus) { this.bonus = bonus; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
