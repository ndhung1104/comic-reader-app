package com.group09.ComicReader.ai.dto;

public class AiChatRequest {
    private String message;
    private String context;

    public AiChatRequest() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}

