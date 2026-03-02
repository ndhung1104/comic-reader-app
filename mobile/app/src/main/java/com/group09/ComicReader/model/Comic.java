package com.group09.ComicReader.model;

import java.util.List;

public class Comic {
    private final int id;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final double rating;
    private final List<String> genres;
    private final String synopsis;
    private final int totalChapters;
    private final Integer progress;
    private final boolean trending;
    private final boolean isNew;

    public Comic(int id, String title, String author, String coverUrl, double rating, List<String> genres,
                 String synopsis, int totalChapters, Integer progress, boolean trending, boolean isNew) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.rating = rating;
        this.genres = genres;
        this.synopsis = synopsis;
        this.totalChapters = totalChapters;
        this.progress = progress;
        this.trending = trending;
        this.isNew = isNew;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverUrl() { return coverUrl; }
    public double getRating() { return rating; }
    public List<String> getGenres() { return genres; }
    public String getSynopsis() { return synopsis; }
    public int getTotalChapters() { return totalChapters; }
    public Integer getProgress() { return progress; }
    public boolean isTrending() { return trending; }
    public boolean isNew() { return isNew; }
}
