package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class RankingViewModel extends ViewModel {

    public enum RankingMode {
        TOP_RATED,
        MOST_VIEWED
    }

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> podiumComics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Comic>> rankedComics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<RankingMode> selectedMode = new MutableLiveData<>(RankingMode.TOP_RATED);

    public void loadData() {
        loadTopRated();
    }

    public void loadTopRated() {
        selectedMode.setValue(RankingMode.TOP_RATED);
        comicRepository.getRankedComics(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                splitRankingList(comics);
            }

            @Override
            public void onError(String error) {
                splitRankingList(new ArrayList<>());
            }
        });
    }

    public void loadMostViewed() {
        selectedMode.setValue(RankingMode.MOST_VIEWED);
        comicRepository.getMostViewedComics(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                splitRankingList(comics);
            }

            @Override
            public void onError(String error) {
                splitRankingList(new ArrayList<>());
            }
        });
    }

    private void splitRankingList(List<Comic> comics) {
        List<Comic> safe = comics == null ? new ArrayList<>() : new ArrayList<>(comics);
        int podiumLimit = Math.min(3, safe.size());
        List<Comic> top = new ArrayList<>(safe.subList(0, podiumLimit));

        int listStart = Math.min(3, safe.size());
        int listEnd = Math.min(10, safe.size());
        List<Comic> list = listStart < listEnd
                ? new ArrayList<>(safe.subList(listStart, listEnd))
                : new ArrayList<>();

        podiumComics.postValue(top);
        rankedComics.postValue(list);
    }

    public LiveData<List<Comic>> getPodiumComics() {
        return podiumComics;
    }

    public LiveData<List<Comic>> getRankedComics() {
        return rankedComics;
    }

    public LiveData<RankingMode> getSelectedMode() {
        return selectedMode;
    }
}
