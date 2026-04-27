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

    public interface ListJobsCallback {
        void onSuccess(@NonNull java.util.List<ImportJobResponse> jobs, int page, int totalPages);

        void onError(@NonNull String message);
    }

    public interface ListComicsCallback {
        void onSuccess(@NonNull java.util.List<com.group09.ComicReader.model.ComicResponse> comics, int page, int totalPages);

        void onError(@NonNull String message);
    }

    public interface CreateComicCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.ComicResponse response);

        void onError(@NonNull String message);
    }

    public interface DeleteComicCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public interface SummaryCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.AiSummaryResponse response);

        void onError(@NonNull String message);
    }

    public interface SummaryListCallback {
        void onSuccess(@NonNull java.util.List<com.group09.ComicReader.model.AiSummaryResponse> responses);

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

    public void getMyJobs(int page, int size, @NonNull ListJobsCallback callback) {
        apiClient.creatorApi().getMyJobs(page, size).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load jobs"));
                    return;
                }
                try {
                    java.util.Map<String, Object> body = response.body();
                    java.util.List<?> itemsRaw = (java.util.List<?>) body.get("items");
                    java.util.List<ImportJobResponse> jobs = new java.util.ArrayList<>();
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    if (itemsRaw != null) {
                        for (Object o : itemsRaw) {
                            String json = gson.toJson(o);
                            jobs.add(gson.fromJson(json, ImportJobResponse.class));
                        }
                    }
                    int p = ((Double) body.getOrDefault("page", 0.0)).intValue();
                    int tp = ((Double) body.getOrDefault("totalPages", 0.0)).intValue();
                    callback.onSuccess(jobs, p, tp);
                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getMyComics(int page, int size, @NonNull ListComicsCallback callback) {
        apiClient.creatorApi().getMyComics(page, size).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load comics"));
                    return;
                }
                try {
                    java.util.Map<String, Object> body = response.body();
                    java.util.List<?> itemsRaw = (java.util.List<?>) body.get("items");
                    java.util.List<com.group09.ComicReader.model.ComicResponse> comics = new java.util.ArrayList<>();
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    if (itemsRaw != null) {
                        for (Object o : itemsRaw) {
                            String json = gson.toJson(o);
                            comics.add(gson.fromJson(json, com.group09.ComicReader.model.ComicResponse.class));
                        }
                    }
                    int p = ((Double) body.getOrDefault("page", 0.0)).intValue();
                    int tp = ((Double) body.getOrDefault("totalPages", 0.0)).intValue();
                    callback.onSuccess(comics, p, tp);
                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void createComic(java.util.Map<String, Object> body, @NonNull CreateComicCallback callback) {
        apiClient.creatorApi().createComic(body).enqueue(new Callback<com.group09.ComicReader.model.ComicResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.group09.ComicReader.model.ComicResponse> call,
                    @NonNull Response<com.group09.ComicReader.model.ComicResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to create comic"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<com.group09.ComicReader.model.ComicResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteComic(long id, @NonNull DeleteComicCallback callback) {
        apiClient.creatorApi().deleteComic(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to delete comic"));
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void generateSummary(long comicId, Long chapterId, @NonNull SummaryCallback callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("comicId", comicId);
        if (chapterId != null) body.put("chapterId", chapterId);

        apiClient.aiApi().generateSummary(body).enqueue(new Callback<com.group09.ComicReader.model.AiSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.group09.ComicReader.model.AiSummaryResponse> call,
                    @NonNull Response<com.group09.ComicReader.model.AiSummaryResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Generation failed"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<com.group09.ComicReader.model.AiSummaryResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void moderateSummary(long id, String status, String reason, @NonNull SummaryCallback callback) {
        apiClient.aiApi().moderateSummary(id, status, reason).enqueue(new Callback<com.group09.ComicReader.model.AiSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.group09.ComicReader.model.AiSummaryResponse> call,
                    @NonNull Response<com.group09.ComicReader.model.AiSummaryResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Moderation failed"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<com.group09.ComicReader.model.AiSummaryResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getSummaryHistory(long comicId, Long chapterId, @NonNull SummaryListCallback callback) {
        apiClient.aiApi().getHistory(comicId, chapterId).enqueue(new Callback<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> call,
                    @NonNull Response<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load history"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
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
