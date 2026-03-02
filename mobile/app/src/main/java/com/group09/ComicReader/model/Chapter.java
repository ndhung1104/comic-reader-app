package com.group09.ComicReader.model;

public class Chapter {
    private final int id;
    private final int number;
    private final String title;
    private final boolean premium;
    private final String releaseDate;

    public Chapter(int id, int number, String title, boolean premium, String releaseDate) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.premium = premium;
        this.releaseDate = releaseDate;
    }

    public int getId() { return id; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public boolean isPremium() { return premium; }
    public String getReleaseDate() { return releaseDate; }
}
