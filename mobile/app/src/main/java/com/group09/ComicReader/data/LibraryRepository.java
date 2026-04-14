package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.common.error.ErrorParser;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.FollowStatusResponse;
import com.group09.ComicReader.model.FollowedComicResponse;
import com.group09.ComicReader.model.LibraryItem;
import com.group09.ComicReader.model.ReadingHistoryRequest;
import com.group09.ComicReader.model.RecentReadResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryRepository {

    public interface LibraryItemsCallback {
        void onSuccess(@NonNull List<LibraryItem> items);

        void onError(@NonNull String message);
    }

    public interface FollowStatusCallback {
        void onSuccess(boolean followed);

        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public LibraryRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getFollowedComics(@NonNull LibraryItemsCallback callback) {
        apiClient.libraryApi().getFollowedComics().enqueue(new Callback<List<FollowedComicResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FollowedComicResponse>> call,
                                   @NonNull Response<List<FollowedComicResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load followed comics"));
                    return;
                }
                List<LibraryItem> items = new ArrayList<>();
                for (FollowedComicResponse item : response.body()) {
                    items.add(item.toLibraryItem());
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(@NonNull Call<List<FollowedComicResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void getRecentReads(@NonNull LibraryItemsCallback callback) {
        apiClient.libraryApi().getRecentReads().enqueue(new Callback<List<RecentReadResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RecentReadResponse>> call,
                                   @NonNull Response<List<RecentReadResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load recent reads"));
                    return;
                }
                List<LibraryItem> items = new ArrayList<>();
                for (RecentReadResponse item : response.body()) {
                    items.add(item.toLibraryItem());
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(@NonNull Call<List<RecentReadResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void getFollowStatus(int comicId, @NonNull FollowStatusCallback callback) {
        apiClient.libraryApi().getFollowStatus(comicId).enqueue(new Callback<FollowStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<FollowStatusResponse> call,
                                   @NonNull Response<FollowStatusResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load follow status"));
                    return;
                }
                callback.onSuccess(response.body().isFollowed());
            }

            @Override
            public void onFailure(@NonNull Call<FollowStatusResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void followComic(int comicId, @NonNull ActionCallback callback) {
        apiClient.libraryApi().followComic(comicId).enqueue(new SimpleActionCallback(callback, "Follow failed"));
    }

    public void unfollowComic(int comicId, @NonNull ActionCallback callback) {
        apiClient.libraryApi().unfollowComic(comicId).enqueue(new SimpleActionCallback(callback, "Unfollow failed"));
    }

    public void recordReadingHistory(int comicId, int chapterId, int pageNumber, @NonNull ActionCallback callback) {
        ReadingHistoryRequest request = new ReadingHistoryRequest((long) comicId, (long) chapterId, pageNumber);
        apiClient.libraryApi().recordReadingHistory(request).enqueue(new Callback<RecentReadResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecentReadResponse> call,
                                   @NonNull Response<RecentReadResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to save reading history"));
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<RecentReadResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private final class SimpleActionCallback implements Callback<FollowStatusResponse> {
        private final ActionCallback callback;
        private final String fallback;

        private SimpleActionCallback(ActionCallback callback, String fallback) {
            this.callback = callback;
            this.fallback = fallback;
        }

        @Override
        public void onResponse(@NonNull Call<FollowStatusResponse> call,
                               @NonNull Response<FollowStatusResponse> response) {
            if (!response.isSuccessful() || response.body() == null) {
                callback.onError(extractErrorMessage(response, fallback));
                return;
            }
            callback.onSuccess();
        }

        @Override
        public void onFailure(@NonNull Call<FollowStatusResponse> call, @NonNull Throwable t) {
            callback.onError(getNetworkMessage(t));
        }
    }

    @NonNull
    private String getNetworkMessage(@NonNull Throwable throwable) {
        return ErrorParser.parseThrowable(throwable, "Network error").getMessage();
    }

    @NonNull
    private String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        String raw = readErrorBody(response);
        return ErrorParser.parseHttpError(response.code(), raw, fallback).getMessage();
    }

    private String readErrorBody(@NonNull Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            String raw = response.errorBody().string();
            return raw == null || raw.trim().isEmpty() ? null : raw;
        } catch (Exception ignored) {
            return null;
        }
    }
}
