package com.group09.ComicReader.auth.dto;

import java.time.LocalDate;
import java.util.List;

public class UserPreferencesResponse {

    private String languageCode;
    private LocalDate dateOfBirth;
    private List<String> preferredGenres;

    public UserPreferencesResponse() {
    }

    public UserPreferencesResponse(String languageCode, LocalDate dateOfBirth, List<String> preferredGenres) {
        this.languageCode = languageCode;
        this.dateOfBirth = dateOfBirth;
        this.preferredGenres = preferredGenres;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public List<String> getPreferredGenres() {
        return preferredGenres;
    }

    public void setPreferredGenres(List<String> preferredGenres) {
        this.preferredGenres = preferredGenres;
    }
}
