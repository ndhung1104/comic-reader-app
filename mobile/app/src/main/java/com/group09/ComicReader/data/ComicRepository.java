package com.group09.ComicReader.data;

import android.content.Context;
import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.data.remote.TranslateApi;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.CategoryPreview;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.CommentPageResponse;
import com.group09.ComicReader.model.CreateCommentRequest;

import java.util.Map;

import java.util.Comparator;
import com.group09.ComicReader.model.PageResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicRepository {

    public interface ComicListCallback {
        void onSuccess(List<Comic> comics);
        void onError(String error);
    }

    public interface ComicCallback {
        void onSuccess(Comic comic);
        void onError(String error);
    }

    public interface CategoryListCallback {
        void onSuccess(List<String> categories);
        void onError(String error);
    }

    public interface CategoryPreviewCallback {
        void onSuccess(CategoryPreview preview);
        void onError(String error);
    }

    public interface CommentListCallback {
        void onSuccess(List<CommentItem> comments);
        void onError(String error);
    }

    public interface CommentCallback {
        void onSuccess(CommentItem comment);
        void onError(String error);
    }

    public interface PagedCommentCallback {
        void onSuccess(List<CommentItem> comments, boolean hasMore);
        void onError(String error);
    }

    public interface TranslateCallback {
        void onSuccess(String translatedTitle, String translatedSynopsis);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    private static ComicRepository instance;
    private final ApiClient apiClient;

    private final List<Chapter> mockedChapters;

    private ComicRepository(Context context) {
        if (context != null) {
            this.apiClient = new ApiClient(context);
        } else {
            this.apiClient = null;
        }
        mockedChapters = buildChapters();
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new ComicRepository(context.getApplicationContext());
        }
    }

    public static ComicRepository getInstance() {
        if (instance == null) {
            instance = new ComicRepository(null);
        }
        return instance;
    }

    /* ========== Comic lists (from real APIs) ========== */

    public void getComics(int page, int size, @NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getComics(null, null, page, size, null).enqueue(new Callback<PageResponse<ComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                    @NonNull Response<PageResponse<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = new ArrayList<>();
                    for (ComicResponse res : response.body().getContent()) {
                        comics.add(res.toComic());
                    }
                    callback.onSuccess(comics);
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getTrendingComics(@NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getTrending(0, 20).enqueue(new Callback<PageResponse<ComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                    @NonNull Response<PageResponse<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = new ArrayList<>();
                    for (ComicResponse res : response.body().getContent()) {
                        comics.add(res.toComic());
                    }
                    callback.onSuccess(comics);
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getDailyUpdates(@NonNull ComicListCallback callback) {
        getComics(0, 10, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                int limit = Math.min(5, comics.size());
                callback.onSuccess(new ArrayList<>(comics.subList(0, limit)));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getRecommended(@NonNull ComicListCallback callback) {
        getComics(0, 50, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                List<Comic> copy = new ArrayList<>(comics);
                Collections.reverse(copy);
                int limit = Math.min(6, copy.size());
                callback.onSuccess(new ArrayList<>(copy.subList(0, limit)));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getRankedComics(@NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getTopRated(0, 100).enqueue(new Callback<PageResponse<ComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                    @NonNull Response<PageResponse<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = new ArrayList<>();
                    for (ComicResponse res : response.body().getContent()) {
                        comics.add(res.toComic());
                    }
                    callback.onSuccess(comics);
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getMostViewedComics(@NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getComics(null, null, 0, 100, "viewCount,desc")
                .enqueue(new Callback<PageResponse<ComicResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                                           @NonNull Response<PageResponse<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = new ArrayList<>();
                            for (ComicResponse res : response.body().getContent()) {
                                comics.add(res.toComic());
                            }
                            comics.sort((left, right) -> Integer.compare(right.getViewCount(), left.getViewCount()));
                            callback.onSuccess(comics);
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getComicsByCategory(@NonNull String categoryId, int size, @NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        String safeCategory = categoryId.trim();
        if (safeCategory.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        int safeSize = Math.max(1, size);
        apiClient.comicApi().getComics(null, safeCategory, 0, safeSize, "viewCount,desc")
                .enqueue(new Callback<PageResponse<ComicResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                                           @NonNull Response<PageResponse<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = new ArrayList<>();
                            for (ComicResponse res : response.body().getContent()) {
                                comics.add(res.toComic());
                            }
                            callback.onSuccess(comics);
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getCategoryPreview(@NonNull String categoryId, int sampleSize, @NonNull CategoryPreviewCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        String safeCategory = categoryId.trim();
        if (safeCategory.isEmpty()) {
            callback.onError("Category is empty");
            return;
        }
        int safeSampleSize = Math.max(1, sampleSize);
        apiClient.comicApi().getComics(null, safeCategory, 0, safeSampleSize, "viewCount,desc")
                .enqueue(new Callback<PageResponse<ComicResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                                           @NonNull Response<PageResponse<ComicResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Error: " + response.code());
                            return;
                        }
                        List<ComicResponse> content = response.body().getContent();
                        String coverUrl = null;
                        String sampleSynopsis = null;
                        boolean hasCompleted = false;
                        boolean hasOngoing = false;

                        if (content != null && !content.isEmpty()) {
                            ComicResponse first = content.get(0);
                            coverUrl = first.getCoverUrl();
                            sampleSynopsis = first.getSynopsis();

                            for (ComicResponse item : content) {
                                String status = item.getStatus() == null ? "" : item.getStatus().trim().toUpperCase(Locale.US);
                                if (status.contains("COMPLETE")) {
                                    hasCompleted = true;
                                }
                                if (status.contains("ONGOING") || status.contains("UPDATING") || status.contains("NEW")) {
                                    hasOngoing = true;
                                }
                            }
                        }
                        if (!hasCompleted && !hasOngoing) {
                            hasOngoing = true;
                        }

                        int totalComics = (int) Math.max(0, Math.min(Integer.MAX_VALUE, response.body().getTotalElements()));
                        callback.onSuccess(new CategoryPreview(
                                safeCategory,
                                safeCategory,
                                coverUrl,
                                totalComics,
                                hasCompleted,
                                hasOngoing,
                                sampleSynopsis));
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getRelatedComics(int comicId, @NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getRelatedComics(comicId, 10).enqueue(new Callback<List<ComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ComicResponse>> call,
                    @NonNull Response<List<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = new ArrayList<>();
                    for (ComicResponse res : response.body()) {
                        comics.add(res.toComic());
                    }
                    callback.onSuccess(comics);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getLibraryComics(@NonNull ComicListCallback callback) {
        getComics(0, 50, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                List<Comic> result = new ArrayList<>();
                for (Comic c : comics) {
                    if (c.getId() % 2 == 0) {
                        result.add(c);
                    }
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getComicById(int comicId, @NonNull ComicCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getComic(comicId).enqueue(new Callback<ComicResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComicResponse> call, @NonNull Response<ComicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().toComic());
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ComicResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    /* ========== Rating & View ========== */

    public void rateComic(int comicId, int score, @NonNull SimpleCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().rateComic(comicId, Map.of("score", score)).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(extractErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private String extractErrorMessage(@NonNull Response<?> response) {
        String errorBodyText = null;
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                errorBodyText = errorBody.string();
            }
        } catch (Exception ignored) {
        }

        if (errorBodyText != null && !errorBodyText.trim().isEmpty()) {
            try {
                JSONObject obj = new JSONObject(errorBodyText);

                JSONObject fields = obj.optJSONObject("fields");
                if (fields != null) {
                    String scoreError = fields.optString("score", null);
                    if (scoreError != null && !scoreError.trim().isEmpty()) {
                        return scoreError;
                    }
                }

                String msg = obj.optString("error", null);
                if (msg != null && !msg.trim().isEmpty()) {
                    return msg;
                }
            } catch (Exception ignored) {
            }
        }

        return "Error: " + response.code();
    }

    public void incrementViewCount(int comicId) {
        if (apiClient == null) return;
        apiClient.comicApi().incrementView(comicId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    /* ========== Translation ========== */

    public void translateComic(int comicId, String targetLang, @NonNull TranslateCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.translateApi().translateComic(comicId, targetLang)
                .enqueue(new Callback<TranslateApi.ComicTranslateResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TranslateApi.ComicTranslateResponse> call,
                            @NonNull Response<TranslateApi.ComicTranslateResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String translatedTitle = response.body().getTranslatedTitle();
                            String translatedSynopsis = response.body().getTranslatedSynopsis();
                            String translationFailure = extractTranslationFailure(translatedTitle, translatedSynopsis);
                            if (translationFailure != null) {
                                callback.onError(translationFailure);
                                return;
                            }
                            callback.onSuccess(
                                    translatedTitle,
                                    translatedSynopsis);
                        } else {
                            callback.onError(extractErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TranslateApi.ComicTranslateResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    private String extractTranslationFailure(String translatedTitle, String translatedSynopsis) {
        String[] translatedFields = {translatedTitle, translatedSynopsis};
        for (String translatedField : translatedFields) {
            if (translatedField == null) {
                continue;
            }
            String normalized = translatedField.trim();
            if (!normalized.startsWith("[Translation")) {
                continue;
            }
            if (normalized.toLowerCase(Locale.US).contains("configured")) {
                return "Translation service is not configured on server";
            }
            return "Translation service is temporarily unavailable";
        }
        return null;
    }

    /* ========== Filters ========== */

    public void getFilters(@NonNull CategoryListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.categoryApi().getCategories().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void searchComics(String query, String filter, @NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        String safeQuery = (query == null || query.trim().isEmpty()) ? null : query.trim();
        String safeFilter = (filter == null || "All".equalsIgnoreCase(filter)) ? null : filter;

        apiClient.comicApi().getComics(safeQuery, safeFilter, 0, 100, null).enqueue(new Callback<PageResponse<ComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<ComicResponse>> call,
                    @NonNull Response<PageResponse<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comic> comics = new ArrayList<>();
                    for (ComicResponse res : response.body().getContent()) {
                        comics.add(res.toComic());
                    }
                    callback.onSuccess(comics);
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public List<Chapter> getChaptersForComic(int comicId) {
        return new ArrayList<>(mockedChapters);
    }

    /* ========== Comments ========== */

    public void getCommentsForComic(int comicId, @NonNull CommentListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.commentApi().getComments(comicId).enqueue(new Callback<List<CommentItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<CommentItem>> call,
                    @NonNull Response<List<CommentItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CommentItem>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getCommentsForComicPaged(int comicId, Integer chapterId, int page, int size,
            @NonNull PagedCommentCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        Long chapterIdLong = chapterId == null || chapterId <= 0 ? null : chapterId.longValue();
        apiClient.commentApi().getCommentsPaged(comicId, page, size, chapterIdLong)
                .enqueue(new Callback<CommentPageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CommentPageResponse> call,
                            @NonNull Response<CommentPageResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Error: " + response.code());
                            return;
                        }
                        List<CommentItem> content = response.body().getContent();
                        callback.onSuccess(content == null ? new ArrayList<>() : content, !response.body().isLast());
                    }

                    @Override
                    public void onFailure(@NonNull Call<CommentPageResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void postComment(int comicId, String content, @NonNull CommentCallback callback) {
        postComment(comicId, null, content, callback);
    }

    public void postComment(int comicId, Integer chapterId, String content, @NonNull CommentCallback callback) {
        postComment(comicId, chapterId, null, content, callback);
    }

    public void postComment(int comicId, Integer chapterId, Long parentCommentId, String content, @NonNull CommentCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        Long chapterIdLong = chapterId == null || chapterId <= 0 ? null : chapterId.longValue();
        CreateCommentRequest body = new CreateCommentRequest(content, "NORMAL", chapterIdLong, parentCommentId);
        apiClient.commentApi().postComment(comicId, body).enqueue(new Callback<CommentItem>() {
            @Override
            public void onResponse(@NonNull Call<CommentItem> call,
                    @NonNull Response<CommentItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    int code = response.code();
                    if (code == 401 || code == 403) {
                        String errorBodyText = null;
                        try {
                            ResponseBody errorBody = response.errorBody();
                            if (errorBody != null) {
                                errorBodyText = errorBody.string();
                            }
                        } catch (Exception ignored) {
                        }

                        if (errorBodyText != null && errorBodyText.toLowerCase(Locale.US).contains("banned")) {
                            callback.onError("Account is banned");
                        } else {
                            callback.onError("Session expired. Please log in again.");
                        }
                        return;
                    }

                    String errorBodyText = null;
                    try {
                        ResponseBody errorBody = response.errorBody();
                        if (errorBody != null) {
                            errorBodyText = errorBody.string();
                        }
                    } catch (Exception ignored) {
                    }

                    if (errorBodyText != null && !errorBodyText.trim().isEmpty()) {
                        try {
                            JSONObject obj = new JSONObject(errorBodyText);
                            String msg = obj.optString("error", null);
                            if (msg != null && !msg.trim().isEmpty()) {
                                callback.onError(msg);
                                return;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    callback.onError("Error: " + code);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentItem> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private List<Chapter> buildChapters() {
        List<Chapter> list = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            boolean premium = i >= 15;
            list.add(new Chapter(i, i, "Chapter " + i, premium, "Mar " + (i < 10 ? "0" + i : i) + ", 2026"));
        }
        return list;
    }
}
