package com.group09.ComicReader.share;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class ShareContent {

    public static final String TYPE_COMIC = "comic";
    public static final String TYPE_CHAPTER = "chapter";

    private final String type;
    private final String title;
    private final String url;
    private final int chapterNumber;

    public ShareContent(@NonNull String type, @NonNull String title, @NonNull String url, int chapterNumber) {
        this.type = normalize(type, TYPE_COMIC);
        this.title = normalize(title, "ComicReader");
        this.url = normalize(url, "");
        this.chapterNumber = Math.max(0, chapterNumber);
    }

    public boolean isChapter() {
        return TYPE_CHAPTER.equals(type);
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    @NonNull
    public String buildCaption(
            @NonNull String comicCaptionTemplate,
            @NonNull String chapterCaptionTemplate,
            @NonNull String chapterFallbackTemplate) {
        if (!isChapter()) {
            return String.format(Locale.getDefault(), comicCaptionTemplate, title);
        }
        if (chapterNumber > 0) {
            return String.format(Locale.getDefault(), chapterCaptionTemplate, title, chapterNumber);
        }
        return String.format(Locale.getDefault(), chapterFallbackTemplate, title);
    }

    @NonNull
    public String buildShareText(
            @NonNull String comicCaptionTemplate,
            @NonNull String chapterCaptionTemplate,
            @NonNull String chapterFallbackTemplate) {
        String caption = buildCaption(comicCaptionTemplate, chapterCaptionTemplate, chapterFallbackTemplate);
        if (url.isEmpty()) {
            return caption;
        }
        return caption + "\n" + url;
    }

    @NonNull
    private static String normalize(String raw, @NonNull String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
