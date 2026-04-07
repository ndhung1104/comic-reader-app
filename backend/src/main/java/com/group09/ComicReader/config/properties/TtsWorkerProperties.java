package com.group09.ComicReader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tts-worker")
public class TtsWorkerProperties {

    private String baseUrl = "http://localhost:8091";
    private int timeoutMs = 300000;
    private int pollIntervalMs = 3000;
    private int maxPollAttempts = 60;
    private String defaultVoice = "af_heart";
    private double defaultSpeed = 1.0;
    private String defaultLang = "auto";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getMaxPollAttempts() {
        return maxPollAttempts;
    }

    public void setMaxPollAttempts(int maxPollAttempts) {
        this.maxPollAttempts = maxPollAttempts;
    }

    public String getDefaultVoice() {
        return defaultVoice;
    }

    public void setDefaultVoice(String defaultVoice) {
        this.defaultVoice = defaultVoice;
    }

    public double getDefaultSpeed() {
        return defaultSpeed;
    }

    public void setDefaultSpeed(double defaultSpeed) {
        this.defaultSpeed = defaultSpeed;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
    }
}
