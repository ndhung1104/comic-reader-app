package com.group09.ComicReader.auth.dto;

public class UserProfileResponse {
    private String email;
    private String fullName;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
