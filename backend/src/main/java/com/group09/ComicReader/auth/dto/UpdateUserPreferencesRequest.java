package com.group09.ComicReader.auth.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class UpdateUserPreferencesRequest {

    @Size(max = 10)
    private String languageCode;

    private LocalDate dateOfBirth;

    @Size(max = 100)
    private List<@Size(max = 100) String> preferredGenres;

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
