package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.List;

public class SearchViewModel extends ViewModel {

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> results = new MutableLiveData<>();
    private final MutableLiveData<List<String>> filters = new MutableLiveData<>();
    private String currentQuery = "";
    private String currentFilter = "All";

    public void loadInitial() {
        filters.setValue(comicRepository.getFilters());
        fetchSearchResults();
    }

    public void updateQuery(String query) {
        currentQuery = query;
        fetchSearchResults();
    }

    public void updateFilter(String filter) {
        currentFilter = filter;
        fetchSearchResults();
    }

    private void fetchSearchResults() {
        comicRepository.searchComics(currentQuery, currentFilter, new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                results.postValue(comics);
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    public LiveData<List<Comic>> getResults() {
        return results;
    }

    public LiveData<List<String>> getFilters() {
        return filters;
    }
}
