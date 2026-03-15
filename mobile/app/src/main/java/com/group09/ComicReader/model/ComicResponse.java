package com.group09.ComicReader.model;

import java.util.List;

public class ComicResponse {
    private Long id;
    private String title;
    private String author;
    private String slug;
    private List<String> genres;
    private String synopsis;
    private String coverUrl;
    private String status;
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Comic toComic() {
        // Map fields to Comic model, using default values if necessary
        int comicId = id != null ? id.intValue() : 0;
        String safeTitle = title != null ? title : "";
        String safeAuthor = author != null ? author : "Unknown";
        String safeCover = coverUrl != null ? coverUrl : "";
        String safeSynopsis = synopsis != null ? synopsis : "No synopsis provided.";
        boolean isTrending = (comicId % 2 == 0) || "TRENDING".equalsIgnoreCase(status);
        boolean isNew = (comicId % 3 == 0) || "NEW".equalsIgnoreCase(status);

        // Mock rating and total chapters as they might not be part of the initial
        // response
        double rating = 4.5;
        int totalChapters = 0;

        return new Comic(comicId, safeTitle, safeAuthor, safeCover, rating, genres, safeSynopsis, totalChapters, null,
                isTrending, isNew);
    }
}
