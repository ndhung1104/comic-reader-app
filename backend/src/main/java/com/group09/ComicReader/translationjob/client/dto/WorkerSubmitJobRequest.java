package com.group09.ComicReader.translationjob.client.dto;

import java.util.List;

public class WorkerSubmitJobRequest {

    private String idempotencyKey;
    private Long chapterId;
    private Long comicId;
    private String sourceLang;
    private String targetLang;
    private List<WorkerPageInput> pages;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getComicId() {
        return comicId;
    }

    public void setComicId(Long comicId) {
        this.comicId = comicId;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }

    public List<WorkerPageInput> getPages() {
        return pages;
    }

    public void setPages(List<WorkerPageInput> pages) {
        this.pages = pages;
    }
}
