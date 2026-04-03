package com.group09.ComicReader.translationjob.client.dto;

import java.util.List;
import java.util.Map;

public class WorkerJobStatusResponse {

    private String jobId;
    private String status;
    private String error;
    private List<WorkerOcrPageText> ocrPages;
    private Map<String, Object> artifacts;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<WorkerOcrPageText> getOcrPages() {
        return ocrPages;
    }

    public void setOcrPages(List<WorkerOcrPageText> ocrPages) {
        this.ocrPages = ocrPages;
    }

    public Map<String, Object> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Map<String, Object> artifacts) {
        this.artifacts = artifacts;
    }
}
