package com.group09.ComicReader.translationjob.client.dto;

import java.util.ArrayList;
import java.util.List;

public class TtsWorkerSynthesizeBatchRequest {

    private String chapterId;
    private String lang;
    private String voice;
    private Double speed;
    private List<TtsWorkerPageInput> pages = new ArrayList<>();

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
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

    public List<TtsWorkerPageInput> getPages() {
        return pages;
    }

    public void setPages(List<TtsWorkerPageInput> pages) {
        this.pages = pages;
    }
}
