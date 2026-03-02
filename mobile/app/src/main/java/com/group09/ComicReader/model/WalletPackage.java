package com.group09.ComicReader.model;

public class WalletPackage {
    private final int coins;
    private final String price;
    private final String bonus;

    public WalletPackage(int coins, String price, String bonus) {
        this.coins = coins;
        this.price = price;
        this.bonus = bonus;
    }

    public int getCoins() { return coins; }
    public String getPrice() { return price; }
    public String getBonus() { return bonus; }
}
