package com.group09.ComicReader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tts-worker")
public class TtsWorkerProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:8091";
    private int timeoutMs = 300000;
    private int pollIntervalMs = 3000;
    private int maxPollAttempts = 60;
    private String defaultVoice = "vi_VN-vais1000-medium";
    private String voiceVi = "vi_VN-vais1000-medium";
    private String voiceEn = "en_US-lessac-medium";
    private String voiceJa = "en_US-lessac-medium";
    private String voiceKo = "en_US-lessac-medium";
    private String fallbackVoice = "en_US-lessac-medium";
    private double defaultSpeed = 1.0;
    private String defaultLang = "auto";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public String getVoiceVi() {
        return voiceVi;
    }

    public void setVoiceVi(String voiceVi) {
        this.voiceVi = voiceVi;
    }

    public String getVoiceEn() {
        return voiceEn;
    }

    public void setVoiceEn(String voiceEn) {
        this.voiceEn = voiceEn;
    }

    public String getVoiceJa() {
        return voiceJa;
    }

    public void setVoiceJa(String voiceJa) {
        this.voiceJa = voiceJa;
    }

    public String getVoiceKo() {
        return voiceKo;
    }

    public void setVoiceKo(String voiceKo) {
        this.voiceKo = voiceKo;
    }

    public String getFallbackVoice() {
        return fallbackVoice;
    }

    public void setFallbackVoice(String fallbackVoice) {
        this.fallbackVoice = fallbackVoice;
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
