package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.AuthResponse;
import com.group09.ComicReader.model.LoginRequest;
import com.group09.ComicReader.model.RegisterRequest;

import org.json.JSONObject;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(@NonNull AuthResponse authResponse);

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;
    private final SessionManager sessionManager;

    public AuthRepository(@NonNull ApiClient apiClient, @NonNull SessionManager sessionManager) {
        this.apiClient = apiClient;
        this.sessionManager = sessionManager;
    }

    public boolean hasToken() {
        return sessionManager.hasToken();
    }

    public void login(String email, String password, @NonNull AuthCallback callback) {
        apiClient.authApi().login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Login failed (" + response.code() + ")"));
                    return;
                }
                AuthResponse body = response.body();
                if (body == null || body.getAccessToken() == null || body.getAccessToken().trim().isEmpty()) {
                    callback.onError("Login failed (empty token)");
                    return;
                }
                sessionManager.saveAuth(body);
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void register(String email, String password, String fullName, @NonNull AuthCallback callback) {
        apiClient.authApi().register(new RegisterRequest(email, password, fullName)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Register failed (" + response.code() + ")"));
                    return;
                }
                AuthResponse body = response.body();
                if (body == null || body.getAccessToken() == null || body.getAccessToken().trim().isEmpty()) {
                    callback.onError("Register failed (empty token)");
                    return;
                }
                sessionManager.saveAuth(body);
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    @NonNull
    private static String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null) return fallback;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) return fallback;

            JSONObject json = new JSONObject(raw);
            if (json.has("error")) {
                String message = json.optString("error", "");
                return message == null || message.trim().isEmpty() ? fallback : humanizeAuthError(message);
            }
            if (json.has("message")) {
                String message = json.optString("message", "");
                return message == null || message.trim().isEmpty() ? fallback : humanizeAuthError(message);
            }
            return fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @NonNull
    private static String humanizeAuthError(@NonNull String message) {
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return message;

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("bad credentials") || lower.equals("bad credential") || lower.contains("bad credentials")) {
            return "Invalid email or password";
        }

        return message;
    }
}
