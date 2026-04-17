package com.group09.ComicReader.model;

import java.util.List;

public class UpdateUserPreferencesRequest {

    private String languageCode;
    private String dateOfBirth;
    private List<String> preferredGenres;

    public UpdateUserPreferencesRequest() {
    }

    public UpdateUserPreferencesRequest(String languageCode, String dateOfBirth, List<String> preferredGenres) {
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public List<String> getPreferredGenres() {
        return preferredGenres;
    }

    public void setPreferredGenres(List<String> preferredGenres) {
        this.preferredGenres = preferredGenres;
    }
}
