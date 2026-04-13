package com.group09.ComicReader.model;

public class CategoryPreview {
    private final String categoryId;
    private final String displayName;
    private final String coverUrl;
    private final int totalComics;
    private final boolean hasCompleted;
    private final boolean hasOngoing;
    private final String sampleSynopsis;

    public CategoryPreview(String categoryId, String displayName, String coverUrl, int totalComics,
                           boolean hasCompleted, boolean hasOngoing, String sampleSynopsis) {
        this.categoryId = categoryId;
        this.displayName = displayName;
        this.coverUrl = coverUrl;
        this.totalComics = totalComics;
        this.hasCompleted = hasCompleted;
        this.hasOngoing = hasOngoing;
        this.sampleSynopsis = sampleSynopsis;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getTotalComics() {
        return totalComics;
    }

    public boolean hasCompleted() {
        return hasCompleted;
    }

    public boolean hasOngoing() {
        return hasOngoing;
    }

    public String getSampleSynopsis() {
        return sampleSynopsis;
    }
}
