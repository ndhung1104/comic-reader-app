package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.model.ReaderPage;

import java.util.List;

public class ReaderViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final ReaderRepository readerRepository;

        public Factory(ReaderRepository readerRepository) {
            this.readerRepository = readerRepository;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ReaderViewModel.class)) {
                return (T) new ReaderViewModel(readerRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final ReaderRepository readerRepository;
    private final MutableLiveData<List<ReaderPage>> pages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReaderViewModel(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    public void loadChapterPages(long chapterId) {
        loading.setValue(true);
        errorMessage.setValue(null);

        readerRepository.getChapterPages(chapterId, new ReaderRepository.PagesCallback() {
            @Override
            public void onSuccess(List<ReaderPage> chapterPages) {
                loading.postValue(false);
                pages.postValue(chapterPages);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public LiveData<List<ReaderPage>> getPages() {
        return pages;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
