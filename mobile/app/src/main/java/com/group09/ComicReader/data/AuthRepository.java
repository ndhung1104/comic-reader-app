package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.AuthResponse;
import com.group09.ComicReader.model.LoginRequest;
import com.group09.ComicReader.model.RegisterRequest;

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
                    callback.onError("Login failed (" + response.code() + ")");
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
                    callback.onError("Register failed (" + response.code() + ")");
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
}
