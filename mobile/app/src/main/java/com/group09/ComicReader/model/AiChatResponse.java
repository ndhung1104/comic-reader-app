package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AiChatResponse implements Serializable {
    @SerializedName("reply")
    private String reply;

    @SerializedName("timestamp")
    private String timestamp;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

