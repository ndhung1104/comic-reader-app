package com.group09.ComicReader.translationjob.client.dto;

public class TtsWorkerPageInput {

    private Integer pageNumber;
    private String text;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
