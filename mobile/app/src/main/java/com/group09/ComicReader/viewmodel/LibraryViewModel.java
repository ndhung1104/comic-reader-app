package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.LibraryRepository;
import com.group09.ComicReader.model.LibraryItem;

import java.util.List;

public class LibraryViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final LibraryRepository libraryRepository;

        public Factory(@NonNull LibraryRepository libraryRepository) {
            this.libraryRepository = libraryRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(LibraryViewModel.class)) {
                return (T) new LibraryViewModel(libraryRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final LibraryRepository libraryRepository;
    private final MutableLiveData<List<LibraryItem>> followedComics = new MutableLiveData<>();
    private final MutableLiveData<List<LibraryItem>> recentReads = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LibraryViewModel(@NonNull LibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    public void loadData() {
        loading.setValue(true);
        errorMessage.setValue(null);
        libraryRepository.getFollowedComics(new LibraryRepository.LibraryItemsCallback() {
            @Override
            public void onSuccess(List<LibraryItem> items) {
                followedComics.postValue(items);
                finishLoadingIfReady();
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                finishLoadingIfReady();
            }
        });

        libraryRepository.getRecentReads(new LibraryRepository.LibraryItemsCallback() {
            @Override
            public void onSuccess(List<LibraryItem> items) {
                recentReads.postValue(items);
                finishLoadingIfReady();
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                finishLoadingIfReady();
            }
        });
    }

    private void finishLoadingIfReady() {
        boolean followedReady = followedComics.getValue() != null || errorMessage.getValue() != null;
        boolean recentReady = recentReads.getValue() != null || errorMessage.getValue() != null;
        if (followedReady && recentReady) {
            loading.postValue(false);
        }
    }

    public LiveData<List<LibraryItem>> getFollowedComics() {
        return followedComics;
    }

    public LiveData<List<LibraryItem>> getRecentReads() {
        return recentReads;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
