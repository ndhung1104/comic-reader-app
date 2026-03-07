package com.group09.ComicReader.model;

public class LoginRequest {

    private String email;

    private String password;

    public String getEmail() {
        return email;
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
