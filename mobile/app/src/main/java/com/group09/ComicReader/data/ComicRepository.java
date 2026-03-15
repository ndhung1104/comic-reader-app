package com.group09.ComicReader.data;

import android.content.Context;
import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.PageResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private static ComicRepository instance;
    private final ApiClient apiClient;

    private final List<Chapter> mockedChapters;
    private final List<CommentItem> mockedComments;

    private ComicRepository(Context context) {
        if (context != null) {
            this.apiClient = new ApiClient(context);
        } else {
            this.apiClient = null;
        }
        mockedChapters = buildChapters();
        mockedComments = buildComments();
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

    public void getComics(int page, int size, @NonNull ComicListCallback callback) {
        if (apiClient == null) {
            callback.onError("ApiClient not initialized");
            return;
        }
        apiClient.comicApi().getComics(page, size, null).enqueue(new Callback<PageResponse<ComicResponse>>() {
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
        getComics(0, 50, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                List<Comic> trending = new ArrayList<>();
                for (Comic c : comics) {
                    if (c.isTrending()) {
                        trending.add(c);
                    }
                }
                callback.onSuccess(trending);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
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

    public List<String> getFilters() {
        return Arrays.asList("All", "Action", "Romance", "Fantasy", "Sci-Fi", "Mystery");
    }

    public void searchComics(String query, String filter, @NonNull ComicListCallback callback) {
        getComics(0, 100, new ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                String safeQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
                String safeFilter = filter == null ? "All" : filter;
                List<Comic> result = new ArrayList<>();
                for (Comic comic : comics) {
                    boolean matchesQuery = safeQuery.isEmpty()
                            || comic.getTitle().toLowerCase(Locale.US).contains(safeQuery);
                    boolean matchesFilter = "All".equals(safeFilter)
                            || (comic.getGenres() != null && comic.getGenres().contains(safeFilter));
                    if (matchesQuery && matchesFilter) {
                        result.add(comic);
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

    public List<Chapter> getChaptersForComic(int comicId) {
        return new ArrayList<>(mockedChapters);
    }

    public List<CommentItem> getCommentsForComic(int comicId) {
        return new ArrayList<>(mockedComments);
    }

    private List<Chapter> buildChapters() {
        List<Chapter> list = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            boolean premium = i >= 15;
            list.add(new Chapter(i, i, "Chapter " + i, premium, "Mar " + (i < 10 ? "0" + i : i) + ", 2026"));
        }
        return list;
    }

    private List<CommentItem> buildComments() {
        return new ArrayList<>(Arrays.asList(
                new CommentItem(1, "MangaFan2024", "", "This chapter was incredible!", "2 hours ago", 234),
                new CommentItem(2, "ComicReader88", "", "The art keeps getting better.", "5 hours ago", 187),
                new CommentItem(3, "WebtoonAddict", "", "Need the next chapter now.", "1 day ago", 342)));
    }
}
