package com.group09.ComicReader.translationjob.client.dto;

public class TtsWorkerAudioPage {

    private Integer pageNumber;
    private String audioBase64;
    private Integer durationMs;
    private Integer sampleRateHz;
    private String format;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(Integer sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
