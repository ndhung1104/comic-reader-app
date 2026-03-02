package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> trendingComic = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> dailyUpdates = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> recommended = new MutableLiveData<>();

    public void loadData() {
        trendingComic.setValue(comicRepository.getTrendingComics());
        dailyUpdates.setValue(comicRepository.getDailyUpdates());
        recommended.setValue(comicRepository.getRecommended());
    }

    public LiveData<List<Comic>> getTrendingComic() { return trendingComic; }
    public LiveData<List<Comic>> getDailyUpdates() { return dailyUpdates; }
    public LiveData<List<Comic>> getRecommended() { return recommended; }
}
