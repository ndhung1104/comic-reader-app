package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> results = new MutableLiveData<>();
    private final MutableLiveData<List<String>> filters = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalPages = new MutableLiveData<>(1);
    private final int pageSize = 25;

    private String currentQuery = "";
    private String currentFilter = "All";

    public void loadInitial() {
        comicRepository.getFilters(new ComicRepository.CategoryListCallback() {
            @Override
            public void onSuccess(List<String> categories) {
                List<String> merged = new ArrayList<>();
                merged.add("All");
                if (categories != null) {
                    merged.addAll(categories);
                }
                filters.postValue(merged);
            }

            @Override
            public void onError(String error) {
            }
        });
        fetchPage(0);
    }

    public void updateQuery(String query) {
        currentQuery = query;
        fetchPage(0);
    }

    public void updateFilter(String filter) {
        currentFilter = filter;
        fetchPage(0);
    }

    private void fetchPage(int page) {
        comicRepository.getComicsPaged(page, pageSize, new ComicRepository.PagedComicCallback() {
            @Override
            public void onSuccess(List<Comic> comics, int pageResult, int totalPagesResult, long totalElements) {
                results.postValue(comics);
                currentPage.postValue(pageResult);
                totalPages.postValue(Math.max(1, totalPagesResult));
            }

            @Override
            public void onError(String error) {
                results.postValue(new ArrayList<>());
            }
        });
    }

    public void nextPage() {
        Integer cur = currentPage.getValue();
        Integer tot = totalPages.getValue();
        if (cur == null)
            cur = 0;
        if (tot == null)
            tot = 1;
        if (cur + 1 < tot) {
            fetchPage(cur + 1);
        }
    }

    public void prevPage() {
        Integer cur = currentPage.getValue();
        if (cur == null)
            cur = 0;
        if (cur > 0) {
            fetchPage(cur - 1);
        }
    }

    /**
     * Jump to a specific page index (0-based).
     */
    public void goToPage(int page) {
        if (page < 0)
            return;
        fetchPage(page);
    }

    public LiveData<List<Comic>> getResults() {
        return results;
    }

    public LiveData<List<String>> getFilters() {
        return filters;
    }

    public LiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public LiveData<Integer> getTotalPages() {
        return totalPages;
    }
}
