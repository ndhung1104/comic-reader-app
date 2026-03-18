package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;

public class CommentItem {

    @SerializedName("id")
    private long id;

    @SerializedName("comicId")
    private long comicId;

    @SerializedName("userId")
    private long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("content")
    private String content;

    @SerializedName("hidden")
    private boolean hidden;

    @SerializedName("createdAt")
    private String createdAt;

    public CommentItem() {
    }

    public CommentItem(long id, String username, String content, String createdAt) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getComicId() { return comicId; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public boolean isHidden() { return hidden; }
    public String getCreatedAt() { return createdAt; }

    public String getTimeAgo() {
        if (createdAt == null || createdAt.isEmpty()) {
            return "";
        }
        try {
            java.time.LocalDateTime commentTime = java.time.LocalDateTime.parse(createdAt);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long minutes = java.time.Duration.between(commentTime, now).toMinutes();

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h ago";
            long days = hours / 24;
            if (days < 30) return days + "d ago";
            return days / 30 + "mo ago";
        } catch (Exception e) {
            return createdAt;
        }
    }
}
