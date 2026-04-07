package com.group09.ComicReader.translationjob.client.dto;

import java.util.ArrayList;
import java.util.List;

public class TtsWorkerSynthesizeBatchResponse {

    private String status;
    private String error;
    private List<TtsWorkerAudioPage> audioPages = new ArrayList<>();

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

    public List<TtsWorkerAudioPage> getAudioPages() {
        return audioPages;
    }

    public void setAudioPages(List<TtsWorkerAudioPage> audioPages) {
        this.audioPages = audioPages;
    }
}
