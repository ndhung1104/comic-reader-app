package com.group09.ComicReader.model;

public class WalletTransaction {
    private final long id;
    private final String type;
    private final int amount;
    private final String date;

    public WalletTransaction(long id, String type, int amount, String date) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public int getAmount() { return amount; }
    public String getDate() { return date; }
}
