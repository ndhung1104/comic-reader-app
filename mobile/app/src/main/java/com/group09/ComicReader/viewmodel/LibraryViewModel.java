package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.Comic;

import java.util.List;

public class LibraryViewModel extends ViewModel {
    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<Comic>> libraryComics = new MutableLiveData<>();

    public void loadData() {
        libraryComics.setValue(comicRepository.getLibraryComics());
    }

    public LiveData<List<Comic>> getLibraryComics() { return libraryComics; }
}
