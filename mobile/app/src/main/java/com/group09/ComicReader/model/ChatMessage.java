package com.group09.ComicReader.model;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String content;
    private String timestamp;
    private boolean isLoading;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.isLoading = false;
    }

    public ChatMessage(String role, String content, boolean isLoading) {
        this.role = role;
        this.content = content;
        this.isLoading = isLoading;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}

