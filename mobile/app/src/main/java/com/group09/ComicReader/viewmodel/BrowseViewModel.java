package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.CategoryPreview;
import com.group09.ComicReader.model.Genre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BrowseViewModel extends ViewModel {

    private static final String FILTER_ALL = "All";
    private static final String FILTER_COMPLETED = "Completed";
    private static final String FILTER_ONGOING = "Ongoing";
    private static final String VIEW_ALL_NAME = "View All";

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Genre>> genres = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final List<Genre> allGenres = new ArrayList<>();

    public LiveData<List<Genre>> getGenres() {
        return genres;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadGenres() {
        loading.postValue(true);
        errorMessage.postValue(null);
        comicRepository.getFilters(new ComicRepository.CategoryListCallback() {
            @Override
            public void onSuccess(List<String> categories) {
                if (categories == null || categories.isEmpty()) {
                    allGenres.clear();
                    genres.postValue(new ArrayList<>());
                    loading.postValue(false);
                    return;
                }
                int limit = Math.min(10, categories.size());
                List<Genre> loaded = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger finished = new AtomicInteger(0);
                for (int i = 0; i < limit; i++) {
                    final int index = i;
                    final String category = categories.get(i);
                    comicRepository.getCategoryPreview(category, 20, new ComicRepository.CategoryPreviewCallback() {
                        @Override
                        public void onSuccess(CategoryPreview preview) {
                            loaded.add(toGenre(index, preview));
                            completeOne(loaded, finished, limit);
                        }

                        @Override
                        public void onError(String error) {
                            loaded.add(toFallbackGenre(index, category));
                            completeOne(loaded, finished, limit);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                loading.postValue(false);
            }
        });
    }

    public void applyFilter(@NonNull String filter) {
        List<Genre> filtered = new ArrayList<>();
        for (Genre genre : allGenres) {
            if (VIEW_ALL_NAME.equalsIgnoreCase(genre.getName())) {
                filtered.add(genre);
                continue;
            }
            if (FILTER_COMPLETED.equalsIgnoreCase(filter)) {
                if (genre.hasCompleted()) filtered.add(genre);
                continue;
            }
            if (FILTER_ONGOING.equalsIgnoreCase(filter)) {
                if (genre.hasOngoing()) filtered.add(genre);
                continue;
            }
            filtered.add(genre);
        }
        genres.postValue(filtered);
    }

    private void completeOne(List<Genre> loaded, AtomicInteger finished, int total) {
        if (finished.incrementAndGet() != total) {
            return;
        }
        loaded.sort(Comparator.comparingInt(Genre::getId));
        List<Genre> merged = new ArrayList<>(loaded);
        merged.add(new Genre(
                merged.size() + 1,
                VIEW_ALL_NAME,
                null,
                merged.size(),
                Genre.LayoutType.SMALL,
                null,
                "Browse all categories",
                "",
                true,
                true
        ));

        allGenres.clear();
        allGenres.addAll(merged);
        applyFilter(FILTER_ALL);
        loading.postValue(false);
    }

    private Genre toGenre(int index, CategoryPreview preview) {
        Genre.LayoutType layoutType = Genre.LayoutType.SMALL;
        String badge = null;
        String description = null;

        if (index == 0) {
            layoutType = Genre.LayoutType.LARGE;
            badge = "TRENDING NOW";
            description = trimDescription(preview.getSampleSynopsis());
        } else if (preview.getDisplayName() != null
                && preview.getDisplayName().toLowerCase().contains("webtoon")) {
            layoutType = Genre.LayoutType.MEDIUM;
            description = trimDescription(preview.getSampleSynopsis());
        }

        return new Genre(
                index + 1,
                preview.getDisplayName(),
                preview.getCoverUrl(),
                preview.getTotalComics(),
                layoutType,
                badge,
                description,
                preview.getCategoryId(),
                preview.hasCompleted(),
                preview.hasOngoing()
        );
    }

    private Genre toFallbackGenre(int index, String category) {
        return new Genre(
                index + 1,
                category,
                null,
                0,
                index == 0 ? Genre.LayoutType.LARGE : Genre.LayoutType.SMALL,
                index == 0 ? "TRENDING NOW" : null,
                null,
                category,
                true,
                true
        );
    }

    private String trimDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() <= 110) {
            return normalized;
        }
        return normalized.substring(0, 107) + "...";
    }
}
