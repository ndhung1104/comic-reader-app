package com.group09.ComicReader.chapter.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public class ChapterAudioPlaylistRequest {

    @Size(max = 32)
    private String sourceLang;

    @Size(max = 32)
    private String lang;

    @Size(max = 128)
    private String voice;

    @DecimalMin("0.5")
    @DecimalMax("2.0")
    private Double speed;

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
}
