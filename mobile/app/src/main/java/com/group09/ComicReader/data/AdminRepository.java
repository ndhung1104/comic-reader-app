package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.UserProfileResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public AdminRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void banUser(long userId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().banUser(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Ban failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("User banned successfully");
            }
            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void unbanUser(long userId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unbanUser(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Unban failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("User unbanned successfully");
            }
            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void hideComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().hideComment(commentId).enqueue(new CommentCallback("Hide successful", "Hide failed", callback));
    }

    public void unhideComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unhideComment(commentId).enqueue(new CommentCallback("Unhide successful", "Unhide failed", callback));
    }

    public void lockComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().lockComment(commentId).enqueue(new CommentCallback("Lock successful", "Lock failed", callback));
    }

    public void unlockComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unlockComment(commentId).enqueue(new CommentCallback("Unlock successful", "Unlock failed", callback));
    }

    public void deleteComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().deleteComment(commentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Delete failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("Delete successful");
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private static class CommentCallback implements Callback<CommentItem> {
        private final String successMsg;
        private final String defaultErrMsg;
        private final SimpleCallback callback;

        public CommentCallback(String successMsg, String defaultErrMsg, SimpleCallback callback) {
            this.successMsg = successMsg;
            this.defaultErrMsg = defaultErrMsg;
            this.callback = callback;
        }

        @Override
        public void onResponse(@NonNull Call<CommentItem> call, @NonNull Response<CommentItem> response) {
            if (!response.isSuccessful()) {
                callback.onError(extractErrorMessage(response, defaultErrMsg + " (" + response.code() + ")"));
                return;
            }
            callback.onSuccess(successMsg);
        }

        @Override
        public void onFailure(@NonNull Call<CommentItem> call, @NonNull Throwable t) {
            callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
        }
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
