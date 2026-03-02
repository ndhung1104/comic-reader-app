package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;

import java.util.List;

public class ComicDetailViewModel extends ViewModel {

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<Comic> comic = new MutableLiveData<>();
    private final MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();

    public void loadData(int comicId) {
        comic.setValue(comicRepository.getComicById(comicId));
        chapters.setValue(comicRepository.getChaptersForComic(comicId));
    }

    public LiveData<Comic> getComic() { return comic; }
    public LiveData<List<Chapter>> getChapters() { return chapters; }
}
