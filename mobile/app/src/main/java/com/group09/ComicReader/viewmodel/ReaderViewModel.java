package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.ReaderAudioPage;
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
    private final MutableLiveData<Boolean> purchaseLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> historySaved = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> purchaseSuccessBalance = new MutableLiveData<>();
    private final MutableLiveData<ComicChapterResponse> freeAccessChapter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> audioLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> audioErrorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<ReaderAudioPage>> audioPages = new MutableLiveData<>();

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

    public void purchaseChapter(long chapterId) {
        purchaseLoading.setValue(true);
        errorMessage.setValue(null);

        readerRepository.purchaseChapter(chapterId, new ReaderRepository.PurchaseChapterCallback() {
            @Override
            public void onSuccess(int newBalance) {
                purchaseLoading.postValue(false);
                purchaseSuccessBalance.postValue(newBalance);
            }

            @Override
            public void onError(@NonNull String message) {
                purchaseLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void recordReadingHistory(int comicId, int chapterId, int pageNumber) {
        readerRepository.recordReadingHistory(comicId, chapterId, pageNumber, new ReaderRepository.HistoryCallback() {
            @Override
            public void onSuccess() {
                historySaved.postValue(true);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void claimFreeChapterAccess(long chapterId) {
        readerRepository.claimFreeAccess(chapterId, new ReaderRepository.FreeAccessCallback() {
            @Override
            public void onSuccess(@NonNull ComicChapterResponse chapter) {
                freeAccessChapter.postValue(chapter);
            }

            @Override
            public void onError(@NonNull String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public LiveData<Boolean> getHistorySaved() {
        return historySaved;
    }

    public LiveData<Boolean> getPurchaseLoading() {
        return purchaseLoading;
    }

    public LiveData<Integer> getPurchaseSuccessBalance() {
        return purchaseSuccessBalance;
    }

    public LiveData<ComicChapterResponse> getFreeAccessChapter() {
        return freeAccessChapter;
    }

    public void createOrGetChapterAudioPlaylist(long chapterId) {
        audioLoading.setValue(true);
        audioErrorMessage.setValue(null);

        readerRepository.createOrGetChapterAudioPlaylist(chapterId, new ReaderRepository.AudioPlaylistCallback() {
            @Override
            public void onSuccess(@NonNull List<ReaderAudioPage> pages) {
                audioLoading.postValue(false);
                audioPages.postValue(pages);
            }

            @Override
            public void onError(@NonNull String message) {
                audioLoading.postValue(false);
                audioErrorMessage.postValue(message);
            }
        });
    }

    public LiveData<Boolean> getAudioLoading() {
        return audioLoading;
    }

    public LiveData<String> getAudioErrorMessage() {
        return audioErrorMessage;
    }

    public LiveData<List<ReaderAudioPage>> getAudioPages() {
        return audioPages;
    }
}
