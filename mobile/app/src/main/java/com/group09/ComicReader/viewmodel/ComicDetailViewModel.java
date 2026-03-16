package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class ComicDetailViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final ComicRepository comicRepository;
        private final ReaderRepository readerRepository;

        public Factory(ComicRepository comicRepository, ReaderRepository readerRepository) {
            this.comicRepository = comicRepository;
            this.readerRepository = readerRepository;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ComicDetailViewModel.class)) {
                return (T) new ComicDetailViewModel(comicRepository, readerRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final ComicRepository comicRepository;
    private final ReaderRepository readerRepository;
    private final MutableLiveData<Comic> comic = new MutableLiveData<>();
    private final MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();
    private final MutableLiveData<Boolean> chapterLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> relatedComics = new MutableLiveData<>();

    public ComicDetailViewModel(ComicRepository comicRepository, ReaderRepository readerRepository) {
        this.comicRepository = comicRepository;
        this.readerRepository = readerRepository;
    }

    public void loadData(int comicId) {
        comicRepository.getComicById(comicId, new ComicRepository.ComicCallback() {
            @Override
            public void onSuccess(Comic fetchedComic) {
                comic.postValue(fetchedComic);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
        comicRepository.getRelatedComics(comicId, new ComicRepository.ComicListCallback() {
            @Override
            public void onSuccess(List<Comic> comics) {
                relatedComics.postValue(comics);
            }

            @Override
            public void onError(String error) {
                relatedComics.postValue(new ArrayList<>());
            }
        });
        chapterLoading.setValue(true);
        readerRepository.getComicChapters(comicId, new ReaderRepository.ChaptersCallback() {
            @Override
            public void onSuccess(List<Chapter> chapterList) {
                chapterLoading.postValue(false);
                chapters.postValue(chapterList);
            }

            @Override
            public void onError(String message) {
                chapterLoading.postValue(false);
                errorMessage.postValue(message);
                chapters.postValue(comicRepository.getChaptersForComic(comicId));
            }
        });
    }

    public LiveData<Comic> getComic() {
        return comic;
    }

    public LiveData<List<Chapter>> getChapters() {
        return chapters;
    }

    public LiveData<Boolean> getChapterLoading() {
        return chapterLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<Comic>> getRelatedComics() {
        return relatedComics;
    }
}
