package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.CategoryPreview;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeViewModel extends ViewModel {
    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> trendingComic = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> topTrendingComics = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> dailyUpdates = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> recommended = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryPreview>> curatedGenres = new MutableLiveData<>(new ArrayList<>());

    public void loadData() {
        comicRepository.getTrendingComics(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                trendingComic.postValue(comics);
                if (comics == null || comics.isEmpty()) {
                    topTrendingComics.postValue(new ArrayList<>());
                    return;
                }
                int limit = Math.min(5, comics.size());
                topTrendingComics.postValue(new ArrayList<>(comics.subList(0, limit)));
            }

            @Override
            public void onError(String error) {
                // handle error or ignore
                topTrendingComics.postValue(new ArrayList<>());
            }
        });

        comicRepository.getDailyUpdates(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                dailyUpdates.postValue(comics);
            }

            @Override
            public void onError(String error) {
            }
        });

        comicRepository.getRecommended(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                recommended.postValue(comics);
            }

            @Override
            public void onError(String error) {
            }
        });

        loadCuratedGenres();
    }

    private void loadCuratedGenres() {
        comicRepository.getFilters(new ComicRepository.CategoryListCallback() {
            @Override
            public void onSuccess(List<String> categories) {
                if (categories == null || categories.isEmpty()) {
                    curatedGenres.postValue(new ArrayList<>());
                    return;
                }
                int limit = Math.min(6, categories.size());
                List<CategoryPreview> previews = new ArrayList<>();
                AtomicInteger finished = new AtomicInteger(0);
                for (int i = 0; i < limit; i++) {
                    String category = categories.get(i);
                    comicRepository.getCategoryPreview(category, 8, new ComicRepository.CategoryPreviewCallback() {
                        @Override
                        public void onSuccess(CategoryPreview preview) {
                            synchronized (previews) {
                                previews.add(preview);
                            }
                            completePreviewBatch(previews, finished, limit);
                        }

                        @Override
                        public void onError(String error) {
                            synchronized (previews) {
                                previews.add(new CategoryPreview(
                                        category,
                                        category,
                                        null,
                                        0,
                                        true,
                                        true,
                                        null));
                            }
                            completePreviewBatch(previews, finished, limit);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                curatedGenres.postValue(new ArrayList<>());
            }
        });
    }

    private void completePreviewBatch(List<CategoryPreview> previews, AtomicInteger finished, int total) {
        if (finished.incrementAndGet() == total) {
            curatedGenres.postValue(new ArrayList<>(previews));
        }
    }

    public LiveData<List<Comic>> getTrendingComic() {
        return trendingComic;
    }

    public LiveData<List<Comic>> getTopTrendingComics() {
        return topTrendingComics;
    }

    public LiveData<List<Comic>> getDailyUpdates() {
        return dailyUpdates;
    }

    public LiveData<List<Comic>> getRecommended() {
        return recommended;
    }

    public LiveData<List<CategoryPreview>> getCuratedGenres() {
        return curatedGenres;
    }
}
