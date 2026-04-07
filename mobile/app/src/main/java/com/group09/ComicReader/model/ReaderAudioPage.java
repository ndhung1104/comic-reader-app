package com.group09.ComicReader.model;

public class ReaderAudioPage {

    private final int pageNumber;
    private final String audioUrl;
    private final int durationMs;

    public ReaderAudioPage(int pageNumber, String audioUrl, int durationMs) {
        this.pageNumber = pageNumber;
        this.audioUrl = audioUrl;
        this.durationMs = durationMs;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public int getDurationMs() {
        return durationMs;
    }
}
