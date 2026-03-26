package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.ChangePasswordRequest;
import com.group09.ComicReader.model.UpdateProfileRequest;
import com.group09.ComicReader.model.UserProfileResponse;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountRepository {

    public interface SimpleCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public interface MeCallback {
        void onSuccess(@NonNull UserProfileResponse me);

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public AccountRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getMe(@NonNull MeCallback callback) {
        apiClient.userApi().getMe().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to load profile (" + response.code() + ")"));
                    return;
                }
                UserProfileResponse body = response.body();
                if (body == null) {
                    callback.onError("Failed to load profile");
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void updateFullName(@NonNull String fullName, @NonNull MeCallback callback) {
        apiClient.userApi().updateMe(new UpdateProfileRequest(fullName)).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to update name (" + response.code() + ")"));
                    return;
                }
                UserProfileResponse body = response.body();
                if (body == null) {
                    callback.onError("Failed to update name");
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void changePassword(@NonNull String currentPassword, @NonNull String newPassword, @NonNull SimpleCallback callback) {
        apiClient.userApi().changePassword(new ChangePasswordRequest(currentPassword, newPassword))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        if (!response.isSuccessful()) {
                            callback.onError(extractErrorMessage(response, "Failed to update password (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
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
                return message == null || message.trim().isEmpty() ? fallback : message;
            }
            if (json.has("message")) {
                String message = json.optString("message", "");
                return message == null || message.trim().isEmpty() ? fallback : message;
            }
            return fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
