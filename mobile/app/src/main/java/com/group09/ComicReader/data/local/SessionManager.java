package com.group09.ComicReader.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.group09.ComicReader.model.AuthResponse;

public class SessionManager {

    private static final String PREFS_NAME = "comic_reader_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "full_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuth(AuthResponse authResponse) {
        if (authResponse == null) return;
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, authResponse.getAccessToken())
                .putString(KEY_TOKEN_TYPE, authResponse.getTokenType())
                .putString(KEY_ROLE, authResponse.getRole())
                .apply();
    }

    public void saveEmail(@Nullable String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public void saveFullName(@Nullable String fullName) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    @Nullable
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getTokenType() {
        return prefs.getString(KEY_TOKEN_TYPE, null);
    }

    @Nullable
    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    @Nullable
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    @Nullable
    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, null);
    }

    public boolean hasToken() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
