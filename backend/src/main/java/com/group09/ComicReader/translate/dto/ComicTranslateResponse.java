package com.group09.ComicReader.translate.dto;

public class ComicTranslateResponse {

    private Long comicId;
    private String translatedTitle;
    private String translatedSynopsis;
    private String targetLang;

    public ComicTranslateResponse() {}

    public ComicTranslateResponse(Long comicId, String translatedTitle, String translatedSynopsis, String targetLang) {
        this.comicId = comicId;
        this.translatedTitle = translatedTitle;
        this.translatedSynopsis = translatedSynopsis;
        this.targetLang = targetLang;
    }

    public Long getComicId() {
        return comicId;
    }

    public void setComicId(Long comicId) {
        this.comicId = comicId;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public void setTranslatedTitle(String translatedTitle) {
        this.translatedTitle = translatedTitle;
    }

    public String getTranslatedSynopsis() {
        return translatedSynopsis;
    }

    public void setTranslatedSynopsis(String translatedSynopsis) {
        this.translatedSynopsis = translatedSynopsis;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }
}
