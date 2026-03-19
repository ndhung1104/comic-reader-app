package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.ReadingHistoryRequest;
import com.group09.ComicReader.model.ReaderPage;
import com.group09.ComicReader.model.ReaderPageResponse;
import com.group09.ComicReader.model.RecentReadResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderRepository {

    public interface ChaptersCallback {
        void onSuccess(@NonNull List<Chapter> chapters);

        void onError(@NonNull String message);
    }

    public interface PagesCallback {
        void onSuccess(@NonNull List<ReaderPage> pages);

        void onError(@NonNull String message);
    }

    public interface HistoryCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public ReaderRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getComicChapters(int comicId, @NonNull ChaptersCallback callback) {
        apiClient.comicApi().getComicChapters(comicId).enqueue(new Callback<List<ComicChapterResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ComicChapterResponse>> call,
                                   @NonNull Response<List<ComicChapterResponse>> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to load chapters (" + response.code() + ")");
                    return;
                }
                List<ComicChapterResponse> body = response.body();
                if (body == null) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                List<Chapter> chapters = new ArrayList<>();
                for (ComicChapterResponse item : body) {
                    chapters.add(mapChapter(item));
                }
                chapters.sort(Comparator.comparingInt(Chapter::getNumber));
                callback.onSuccess(chapters);
            }

            @Override
            public void onFailure(@NonNull Call<List<ComicChapterResponse>> call, @NonNull Throwable throwable) {
                callback.onError(throwable.getMessage() == null ? "Network error" : throwable.getMessage());
            }
        });
    }

    public void getChapterPages(long chapterId, @NonNull PagesCallback callback) {
        apiClient.chapterApi().getChapterPages(chapterId).enqueue(new Callback<List<ReaderPageResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReaderPageResponse>> call,
                                   @NonNull Response<List<ReaderPageResponse>> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to load pages (" + response.code() + ")");
                    return;
                }
                List<ReaderPageResponse> body = response.body();
                if (body == null) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                List<ReaderPage> pages = new ArrayList<>();
                for (ReaderPageResponse item : body) {
                    int pageNumber = item.getPageNumber() == null ? 0 : item.getPageNumber();
                    int imageWidth = item.getImageWidth() == null ? 0 : item.getImageWidth();
                    int imageHeight = item.getImageHeight() == null ? 0 : item.getImageHeight();
                    pages.add(new ReaderPage(
                            pageNumber,
                            ApiClient.toAbsoluteUrl(item.getImageUrl()),
                            imageWidth,
                            imageHeight));
                }
                pages.sort(Comparator.comparingInt(ReaderPage::getPageNumber));
                callback.onSuccess(pages);
            }

            @Override
            public void onFailure(@NonNull Call<List<ReaderPageResponse>> call, @NonNull Throwable throwable) {
                callback.onError(throwable.getMessage() == null ? "Network error" : throwable.getMessage());
            }
        });
    }

    public void recordReadingHistory(int comicId, int chapterId, int pageNumber, @NonNull HistoryCallback callback) {
        ReadingHistoryRequest request = new ReadingHistoryRequest((long) comicId, (long) chapterId, pageNumber);
        apiClient.libraryApi().recordReadingHistory(request).enqueue(new Callback<RecentReadResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecentReadResponse> call,
                                   @NonNull Response<RecentReadResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 401 || response.code() == 403) {
                        callback.onError("Session expired. Please log in again.");
                    } else {
                        callback.onError("Failed to save reading history (" + response.code() + ")");
                    }
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<RecentReadResponse> call, @NonNull Throwable throwable) {
                callback.onError(throwable.getMessage() == null ? "Network error" : throwable.getMessage());
            }
        });
    }

    private Chapter mapChapter(ComicChapterResponse response) {
        int chapterId = response.getId() == null ? 0 : response.getId().intValue();
        int chapterNumber = response.getChapterNumber() == null ? 0 : response.getChapterNumber();
        boolean premium = response.isPremium();
        boolean unlocked = !premium || response.isUnlocked();

        String title = response.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "Chapter " + chapterNumber;
        }

        String meta = premium ? "Premium" : "Free";
        if (premium && response.getPrice() != null && response.getPrice() > 0) {
            meta = response.getPrice() + " coins";
        }

        return new Chapter(chapterId, chapterNumber, title, premium, meta, unlocked);
    }
}
