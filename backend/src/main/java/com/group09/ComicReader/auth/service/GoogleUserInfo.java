package com.group09.ComicReader.auth.service;

public class GoogleUserInfo {

    private final String email;
    private final String fullName;
    private final boolean emailVerified;

    public GoogleUserInfo(String email, String fullName, boolean emailVerified) {
        this.email = email;
        this.fullName = fullName;
        this.emailVerified = emailVerified;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
