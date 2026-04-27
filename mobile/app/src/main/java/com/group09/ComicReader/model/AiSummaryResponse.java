package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AiSummaryResponse implements Serializable {
    @SerializedName("id")
    private long id;

    @SerializedName("comicId")
    private long comicId;

    @SerializedName("chapterId")
    private Long chapterId;

    @SerializedName("content")
    private String content;

    @SerializedName("status")
    private String status;

    @SerializedName("moderationReason")
    private String moderationReason;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getComicId() { return comicId; }
    public void setComicId(long comicId) { this.comicId = comicId; }

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
