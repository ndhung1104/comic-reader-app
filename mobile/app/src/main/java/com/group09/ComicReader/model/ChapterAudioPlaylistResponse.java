package com.group09.ComicReader.model;

import java.util.List;

public class ChapterAudioPlaylistResponse {

    private Long chapterId;
    private String status;
    private String sourceLang;
    private String lang;
    private String voice;
    private Double speed;
    private List<ChapterAudioPageResponse> audioPages;

    public Long getChapterId() {
        return chapterId;
    }

    public String getStatus() {
        return status;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public String getLang() {
        return lang;
    }

    public String getVoice() {
        return voice;
    }

    public Double getSpeed() {
        return speed;
    }

    public List<ChapterAudioPageResponse> getAudioPages() {
        return audioPages;
    }
}
