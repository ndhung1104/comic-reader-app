package com.group09.ComicReader.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.R;
import com.group09.ComicReader.common.error.ErrorParser;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.data.remote.TranslateApi;
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
import java.util.Set;

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
    @Nullable
    private final Context appContext;

    private ComicRepository(Context context) {
        this.appContext = context == null ? null : context.getApplicationContext();
        if (context != null) {
            this.apiClient = new ApiClient(context);
        } else {
            this.apiClient = null;
        }
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
                    callback.onSuccess(applyUserContentFilter(comics));
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
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
                    callback.onSuccess(applyUserContentFilter(comics));
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
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

                Set<String> preferredGenres = getPreferredGenres();
                if (!preferredGenres.isEmpty() && !copy.isEmpty()) {
                    List<Comic> matched = new ArrayList<>();
                    List<Comic> rest = new ArrayList<>();
                    for (Comic comic : copy) {
                        if (hasAnyPreferredGenre(comic, preferredGenres)) {
                            matched.add(comic);
                        } else {
                            rest.add(comic);
                        }
                    }
                    copy.clear();
                    copy.addAll(matched);
                    copy.addAll(rest);
                }

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
                    callback.onSuccess(applyUserContentFilter(comics));
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
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
                            callback.onSuccess(applyUserContentFilter(comics));
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                        callback.onError(getNetworkMessage(t));
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
                            callback.onSuccess(applyUserContentFilter(comics));
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                        callback.onError(getNetworkMessage(t));
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
                        callback.onError(getNetworkMessage(t));
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
                    callback.onSuccess(applyUserContentFilter(comics));
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void getLibraryComics(@NonNull ComicListCallback callback) {
        getComics(0, 50, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                callback.onSuccess(comics);
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
                callback.onError(getNetworkMessage(t));
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
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private String extractErrorMessage(@NonNull Response<?> response) {
        String errorBodyText = readErrorBody(response);

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

        return ErrorParser.parseHttpError(response.code(), errorBodyText, "Error: " + response.code()).getMessage();
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
                            if (response.code() == 503) {
                                callback.onError(getStringOrDefault(R.string.reader_translate_service_maintenance,
                                        "Translation service is temporarily under maintenance."));
                                return;
                            }
                            callback.onError(extractErrorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TranslateApi.ComicTranslateResponse> call, @NonNull Throwable t) {
                        callback.onError(getNetworkMessage(t));
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
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    @NonNull
    private String getStringOrDefault(int stringRes, @NonNull String fallback) {
        if (appContext == null) {
            return fallback;
        }
        try {
            String value = appContext.getString(stringRes);
            return value == null || value.trim().isEmpty() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @NonNull
    private List<Comic> applyUserContentFilter(@NonNull List<Comic> comics) {
        if (appContext == null || comics.isEmpty()) {
            return comics;
        }
        AppSettingsStore settings = new AppSettingsStore(appContext);
        if (settings.isAllowMatureContent()) {
            return comics;
        }
        List<Comic> filtered = new ArrayList<>();
        for (Comic comic : comics) {
            if (comic == null) {
                continue;
            }
            if (!isMatureComic(comic)) {
                filtered.add(comic);
            }
        }
        return filtered;
    }

    @NonNull
    private Set<String> getPreferredGenres() {
        if (appContext == null) {
            return Collections.emptySet();
        }
        return new AppSettingsStore(appContext).getPreferredGenres();
    }

    private boolean hasAnyPreferredGenre(@NonNull Comic comic, @NonNull Set<String> preferred) {
        if (preferred.isEmpty()) {
            return false;
        }
        List<String> genres = comic.getGenres();
        if (genres == null || genres.isEmpty()) {
            return false;
        }
        for (String genre : genres) {
            if (genre == null) {
                continue;
            }
            String normalized = genre.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            for (String wanted : preferred) {
                if (wanted != null && wanted.trim().equalsIgnoreCase(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMatureComic(@NonNull Comic comic) {
        List<String> genres = comic.getGenres();
        if (genres == null || genres.isEmpty()) {
            return false;
        }
        for (String genre : genres) {
            if (genre == null) {
                continue;
            }
            String normalized = genre.trim().toLowerCase(Locale.US);
            if (normalized.isEmpty()) {
                continue;
            }

            // Match both "name" and "slug" styles (e.g., "Adult" vs "adult").
            if (normalized.contains("18+")
                    || normalized.equals("18")
                    || normalized.contains("r18")
                    || normalized.contains("r-18")
                    || normalized.contains("mature")
                    || normalized.contains("adult")
                    || normalized.contains("nsfw")
                    || normalized.contains("smut")
                    || normalized.contains("ecchi")
                    || normalized.contains("hentai")
                    || normalized.contains("porn")
                    || normalized.contains("sex")) {
                return true;
            }
        }
        return false;
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
                    callback.onSuccess(applyUserContentFilter(comics));
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<ComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
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
                callback.onError(getNetworkMessage(t));
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
                        callback.onError(getNetworkMessage(t));
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
                    String errorBodyText = readErrorBody(response);

                    if (ErrorParser.isTokenExpiredStatus(code)) {
                        if (errorBodyText != null && errorBodyText.toLowerCase(Locale.US).contains("banned")) {
                            callback.onError("Account is banned");
                        } else {
                            callback.onError(ErrorParser.parseHttpError(code, errorBodyText, "Error: " + code).getMessage());
                        }
                        return;
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

                    callback.onError(ErrorParser.parseHttpError(code, errorBodyText, "Error: " + code).getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentItem> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    @NonNull
    private String getNetworkMessage(@Nullable Throwable throwable) {
        if (throwable == null) {
            return "Network error";
        }
        return ErrorParser.parseThrowable(throwable, "Network error").getMessage();
    }

    @Nullable
    private String readErrorBody(@NonNull Response<?> response) {
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody == null) {
                return null;
            }
            String errorBodyText = errorBody.string();
            return errorBodyText == null || errorBodyText.trim().isEmpty() ? null : errorBodyText;
        } catch (Exception ignored) {
            return null;
        }
    }

}
