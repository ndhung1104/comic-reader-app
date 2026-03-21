package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

    @SerializedName("locked")
    private boolean locked;

    @SerializedName("sourceType")
    private String sourceType;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("chapterId")
    private Long chapterId;

    @SerializedName("chapterNumber")
    private Integer chapterNumber;

    @SerializedName("chapterTitle")
    private String chapterTitle;

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
    public boolean isLocked() { return locked; }
    public String getSourceType() { return sourceType; }
    public String getCreatedAt() { return createdAt; }

    public Long getChapterId() { return chapterId; }
    public Integer getChapterNumber() { return chapterNumber; }
    public String getChapterTitle() { return chapterTitle; }

    public String getTimeAgo() {
        if (createdAt == null || createdAt.isEmpty()) {
            return "";
        }
        try {
            Instant commentInstant = parseCreatedAtToInstant(createdAt);
            Instant nowInstant = Instant.now();

            Duration duration = Duration.between(commentInstant, nowInstant);
            if (duration.isNegative()) {
                return "Just now";
            }

            long minutes = duration.toMinutes();
            if (minutes < 1) {
                return "Just now";
            }
            if (minutes < 60) {
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            }

            long hours = duration.toHours();
            if (hours < 1) {
                return "Just now";
            }
            if (hours < 24) {
                return hours + "h ago";
            }

            long days = duration.toDays();
            if (days < 10) {
                return days + "d ago";
            }

            ZoneId vietnam = ZoneId.of("Asia/Ho_Chi_Minh");
            ZonedDateTime vnTime = commentInstant.atZone(vietnam);
            return vnTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " VN";
        } catch (Exception e) {
            return createdAt;
        }
    }

    private static Instant parseCreatedAtToInstant(String raw) {
        // Backend commonly returns ISO LocalDateTime without timezone; treat it as UTC to avoid
        // device-local interpretation (which makes times look wrong in Vietnam).
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
        }

        LocalDateTime local = LocalDateTime.parse(raw);
        return local.toInstant(ZoneOffset.UTC);
    }
}
