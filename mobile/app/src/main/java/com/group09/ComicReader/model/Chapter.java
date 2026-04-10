package com.group09.ComicReader.model;

public class Chapter {
    private final int id;
    private final int number;
    private final String title;
    private final boolean premium;
    private final String releaseDate;
    private final boolean unlocked;
    private final int price;

    public Chapter(int id, int number, String title, boolean premium, String releaseDate) {
        this(id, number, title, premium, releaseDate, !premium, 0);
    }

    public Chapter(int id, int number, String title, boolean premium, String releaseDate, boolean unlocked) {
        this(id, number, title, premium, releaseDate, unlocked, 0);
    }

    public Chapter(int id, int number, String title, boolean premium, String releaseDate, boolean unlocked, int price) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.premium = premium;
        this.releaseDate = releaseDate;
        this.unlocked = unlocked;
        this.price = price;
    }

    public int getId() { return id; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public boolean isPremium() { return premium; }
    public String getReleaseDate() { return releaseDate; }
    public boolean isUnlocked() { return unlocked; }
    public int getPrice() { return price; }
}
