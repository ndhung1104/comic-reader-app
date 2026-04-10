package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.PurchaseChapterRequest;
import com.group09.ComicReader.model.ReadingHistoryRequest;
import com.group09.ComicReader.model.ReaderPage;
import com.group09.ComicReader.model.ReaderPageResponse;
import com.group09.ComicReader.model.RecentReadResponse;
import com.group09.ComicReader.model.WalletResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderRepository {

    public interface ChapterCallback {
        void onSuccess(@NonNull ComicChapterResponse chapter);

        void onError(@NonNull String message);
    }

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

    public interface PurchaseChapterCallback {
        void onSuccess(int newBalance);

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public ReaderRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getChapterById(long chapterId, @NonNull ChapterCallback callback) {
        apiClient.chapterApi().getChapter(chapterId).enqueue(new Callback<ComicChapterResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComicChapterResponse> call,
                                   @NonNull Response<ComicChapterResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load chapter (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<ComicChapterResponse> call, @NonNull Throwable throwable) {
                callback.onError(getNetworkMessage(throwable));
            }
        });
    }

    public void getComicChapters(int comicId, @NonNull ChaptersCallback callback) {
        apiClient.comicApi().getComicChapters(comicId).enqueue(new Callback<List<ComicChapterResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ComicChapterResponse>> call,
                                   @NonNull Response<List<ComicChapterResponse>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to load chapters (" + response.code() + ")"));
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
                callback.onError(getNetworkMessage(throwable));
            }
        });
    }

    public void getChapterPages(long chapterId, @NonNull PagesCallback callback) {
        apiClient.chapterApi().getChapterPages(chapterId).enqueue(new Callback<List<ReaderPageResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReaderPageResponse>> call,
                                   @NonNull Response<List<ReaderPageResponse>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to load pages (" + response.code() + ")"));
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
                callback.onError(getNetworkMessage(throwable));
            }
        });
    }

    public void purchaseChapter(long chapterId, @NonNull PurchaseChapterCallback callback) {
        PurchaseChapterRequest request = new PurchaseChapterRequest(chapterId, "COIN");
        apiClient.walletApi().purchaseChapter(request).enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletResponse> call, @NonNull Response<WalletResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Purchase failed (" + response.code() + ")"));
                    return;
                }

                Integer balance = response.body().getCoinBalance();
                callback.onSuccess(balance == null ? 0 : balance);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable throwable) {
                callback.onError(getNetworkMessage(throwable));
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
                callback.onError(getNetworkMessage(throwable));
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

        int price = response.getPrice() == null ? 0 : response.getPrice();
        return new Chapter(chapterId, chapterNumber, title, premium, meta, unlocked, price);
    }

    @NonNull
    private String getNetworkMessage(@NonNull Throwable throwable) {
        return throwable.getMessage() == null ? "Network error" : throwable.getMessage();
    }

    @NonNull
    private String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return fallback;
            }
            JSONObject json = new JSONObject(raw);
            String message = json.optString("error", json.optString("message", fallback));
            return message == null || message.trim().isEmpty() ? fallback : message;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
