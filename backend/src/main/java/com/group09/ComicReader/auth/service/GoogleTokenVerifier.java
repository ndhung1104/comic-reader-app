package com.group09.ComicReader.auth.service;

public interface GoogleTokenVerifier {

    GoogleUserInfo verify(String idToken);
}
