package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.CreatorRequestResponse;
import com.group09.ComicReader.model.ImportJobResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatorRepository {

    private final ApiClient apiClient;

    public CreatorRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public interface EnqueueCallback {
        void onSuccess(@NonNull ImportJobResponse response);

        void onError(@NonNull String message);
    }

    public interface JobStatusCallback {
        void onSuccess(@NonNull ImportJobResponse response);

        void onError(@NonNull String message);
    }

    public interface CreatorRequestCallback {
        void onSuccess(@NonNull CreatorRequestResponse response);

        void onError(@NonNull String message);
    }

    public void enqueueImport(@NonNull String sourceUrl, @NonNull String sourceType,
            @NonNull EnqueueCallback callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("sourceUrl", sourceUrl);
        body.put("sourceType", sourceType);

        apiClient.creatorApi().importComic(body).enqueue(new Callback<ImportJobResponse>() {
            @Override
            public void onResponse(@NonNull Call<ImportJobResponse> call,
                    @NonNull Response<ImportJobResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Enqueue failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ImportJobResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void requestCreator(@NonNull String message, @NonNull CreatorRequestCallback callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("message", message);

        apiClient.creatorApi().requestCreator(body).enqueue(new Callback<CreatorRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<CreatorRequestResponse> call, @NonNull Response<CreatorRequestResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Request failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CreatorRequestResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void getMyRequest(@NonNull CreatorRequestCallback callback) {
        apiClient.creatorApi().getMyRequest().enqueue(new Callback<CreatorRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<CreatorRequestResponse> call, @NonNull Response<CreatorRequestResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load request (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CreatorRequestResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void getJobStatus(long jobId, @NonNull JobStatusCallback callback) {
        apiClient.creatorApi().getJob(jobId).enqueue(new Callback<ImportJobResponse>() {
            @Override
            public void onResponse(@NonNull Call<ImportJobResponse> call,
                    @NonNull Response<ImportJobResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load job (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ImportJobResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    @NonNull
    private static String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null)
                return fallback;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty())
                return fallback;

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
