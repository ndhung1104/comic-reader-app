package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> trendingComic = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> topTrendingComics = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> dailyUpdates = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> recommended = new MutableLiveData<>();

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
}
