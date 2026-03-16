package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class RankingViewModel extends ViewModel {

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> rankedComics = new MutableLiveData<>();

    public void loadData() {
        comicRepository.getRankedComics(new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                rankedComics.postValue(comics);
            }

            @Override
            public void onError(String error) {
                rankedComics.postValue(new ArrayList<>());
            }
        });
    }

    public LiveData<List<Comic>> getRankedComics() {
        return rankedComics;
    }
}
