package com.group09.ComicReader.ai.dto;

public class AiChatResponse {
    private String reply;
    private String timestamp;

    public AiChatResponse() {}

    public AiChatResponse(String reply, String timestamp) {
        this.reply = reply;
        this.timestamp = timestamp;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

