package com.group09.ComicReader.model;

public class ChapterAudioPageResponse {

    private Integer pageNumber;
    private String audioUrl;
    private Integer durationMs;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public Integer getDurationMs() {
        return durationMs;
    }
}
