package com.group09.ComicReader.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.guardrails")
public class AiGuardrailProperties {

    private boolean enabled = true;
    private int translatePerActorPerDay = 30;
    private int translatePerDay = 500;
    private int translationJobPerActorPerDay = 8;
    private int translationJobPerDay = 120;
    private int audioPlaylistPerActorPerDay = 10;
    private int audioPlaylistPerDay = 200;
    private int aiSummaryPerActorPerDay = 50;
    private int aiSummaryPerDay = 500;
    private int chatPerActorPerDay = 30;
    private int chatPerDay = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTranslatePerActorPerDay() {
        return translatePerActorPerDay;
    }

    public void setTranslatePerActorPerDay(int translatePerActorPerDay) {
        this.translatePerActorPerDay = translatePerActorPerDay;
    }

    public int getTranslatePerDay() {
        return translatePerDay;
    }

    public void setTranslatePerDay(int translatePerDay) {
        this.translatePerDay = translatePerDay;
    }

    public int getTranslationJobPerActorPerDay() {
        return translationJobPerActorPerDay;
    }

    public void setTranslationJobPerActorPerDay(int translationJobPerActorPerDay) {
        this.translationJobPerActorPerDay = translationJobPerActorPerDay;
    }

    public int getTranslationJobPerDay() {
        return translationJobPerDay;
    }

    public void setTranslationJobPerDay(int translationJobPerDay) {
        this.translationJobPerDay = translationJobPerDay;
    }

    public int getAudioPlaylistPerActorPerDay() {
        return audioPlaylistPerActorPerDay;
    }

    public void setAudioPlaylistPerActorPerDay(int audioPlaylistPerActorPerDay) {
        this.audioPlaylistPerActorPerDay = audioPlaylistPerActorPerDay;
    }

    public int getAudioPlaylistPerDay() {
        return audioPlaylistPerDay;
    }

    public void setAudioPlaylistPerDay(int audioPlaylistPerDay) {
        this.audioPlaylistPerDay = audioPlaylistPerDay;
    }

    public int getAiSummaryPerActorPerDay() {
        return aiSummaryPerActorPerDay;
    }

    public void setAiSummaryPerActorPerDay(int aiSummaryPerActorPerDay) {
        this.aiSummaryPerActorPerDay = aiSummaryPerActorPerDay;
    }

    public int getAiSummaryPerDay() {
        return aiSummaryPerDay;
    }

    public void setAiSummaryPerDay(int aiSummaryPerDay) {
        this.aiSummaryPerDay = aiSummaryPerDay;
    }

    public int getChatPerActorPerDay() {
        return chatPerActorPerDay;
    }

    public void setChatPerActorPerDay(int chatPerActorPerDay) {
        this.chatPerActorPerDay = chatPerActorPerDay;
    }

    public int getChatPerDay() {
        return chatPerDay;
    }

    public void setChatPerDay(int chatPerDay) {
        this.chatPerDay = chatPerDay;
    }
}
