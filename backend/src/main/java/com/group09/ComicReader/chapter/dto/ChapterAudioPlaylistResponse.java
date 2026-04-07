package com.group09.ComicReader.chapter.dto;

import java.util.ArrayList;
import java.util.List;

public class ChapterAudioPlaylistResponse {

    private Long chapterId;
    private String status;
    private String sourceLang;
    private String lang;
    private String voice;
    private Double speed;
    private List<ChapterAudioPageResponse> audioPages = new ArrayList<>();

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public List<ChapterAudioPageResponse> getAudioPages() {
        return audioPages;
    }

    public void setAudioPages(List<ChapterAudioPageResponse> audioPages) {
        this.audioPages = audioPages;
    }
}
