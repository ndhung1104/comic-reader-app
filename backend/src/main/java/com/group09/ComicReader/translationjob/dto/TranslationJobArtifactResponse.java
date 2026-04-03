package com.group09.ComicReader.translationjob.dto;

import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;

import java.util.List;
import java.util.Map;

public class TranslationJobArtifactResponse {

    private Long jobId;
    private String externalJobId;
    private TranslationJobStatus status;
    private Map<String, Object> artifacts;
    private List<OcrPageTextResponse> ocrPages;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public void setExternalJobId(String externalJobId) {
        this.externalJobId = externalJobId;
    }

    public TranslationJobStatus getStatus() {
        return status;
    }

    public void setStatus(TranslationJobStatus status) {
        this.status = status;
    }

    public Map<String, Object> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Map<String, Object> artifacts) {
        this.artifacts = artifacts;
    }

    public List<OcrPageTextResponse> getOcrPages() {
        return ocrPages;
    }

    public void setOcrPages(List<OcrPageTextResponse> ocrPages) {
        this.ocrPages = ocrPages;
    }
}
