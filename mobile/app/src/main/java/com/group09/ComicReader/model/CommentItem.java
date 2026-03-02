package com.group09.ComicReader.model;

public class CommentItem {
    private final int id;
    private final String username;
    private final String avatarUrl;
    private final String text;
    private final String timestamp;
    private final int likes;

    public CommentItem(int id, String username, String avatarUrl, String text, String timestamp, int likes) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.text = text;
        this.timestamp = timestamp;
        this.likes = likes;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getText() { return text; }
    public String getTimestamp() { return timestamp; }
    public int getLikes() { return likes; }
}
