package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.LibraryRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class ComicDetailViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final ComicRepository comicRepository;
        private final ReaderRepository readerRepository;
        private final LibraryRepository libraryRepository;

        public Factory(ComicRepository comicRepository, ReaderRepository readerRepository,
                       LibraryRepository libraryRepository) {
            this.comicRepository = comicRepository;
            this.readerRepository = readerRepository;
            this.libraryRepository = libraryRepository;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ComicDetailViewModel.class)) {
                return (T) new ComicDetailViewModel(comicRepository, readerRepository, libraryRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final ComicRepository comicRepository;
    private final ReaderRepository readerRepository;
    private final LibraryRepository libraryRepository;
    private final MutableLiveData<Comic> comic = new MutableLiveData<>();
    private final MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();
    private final MutableLiveData<Boolean> chapterLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> purchaseLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Chapter> purchasedChapter = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Comic>> relatedComics = new MutableLiveData<>();
    private final MutableLiveData<Boolean> followed = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> followLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> followSuccessMessage = new MutableLiveData<>();

    /* Translation state */
    private final MutableLiveData<String> translatedTitle = new MutableLiveData<>();
    private final MutableLiveData<String> translatedSynopsis = new MutableLiveData<>();
    private final MutableLiveData<Boolean> translating = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showingTranslation = new MutableLiveData<>(false);

    /* Rating state */
    private final MutableLiveData<Boolean> rateLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> rateMessage = new MutableLiveData<>();

    public ComicDetailViewModel(ComicRepository comicRepository, ReaderRepository readerRepository,
                                LibraryRepository libraryRepository) {
        this.comicRepository = comicRepository;
        this.readerRepository = readerRepository;
        this.libraryRepository = libraryRepository;
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
        loadChapters(comicId);

        /* Increment view count */
        comicRepository.incrementViewCount(comicId);
    }

    public void purchaseChapter(int comicId, @NonNull Chapter chapter) {
        purchaseLoading.setValue(true);
        errorMessage.setValue(null);

        readerRepository.purchaseChapter(chapter.getId(), new ReaderRepository.PurchaseChapterCallback() {
            @Override
            public void onSuccess(int newBalance) {
                purchaseLoading.postValue(false);
                purchasedChapter.postValue(chapter);
                loadChapters(comicId);
            }

            @Override
            public void onError(@NonNull String message) {
                purchaseLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void loadChapters(int comicId) {
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
            }
        });
    }

    /* ========== Translation ========== */

    public void translateComic(int comicId, String targetLang) {
        if (Boolean.TRUE.equals(showingTranslation.getValue())) {
            showingTranslation.setValue(false);
            return;
        }
        if (translatedTitle.getValue() != null) {
            showingTranslation.setValue(true);
            return;
        }
        translating.setValue(true);
        comicRepository.translateComic(comicId, targetLang, new ComicRepository.TranslateCallback() {
            @Override
            public void onSuccess(String title, String synopsis) {
                translatedTitle.postValue(title);
                translatedSynopsis.postValue(synopsis);
                translating.postValue(false);
                showingTranslation.postValue(true);
            }

            @Override
            public void onError(String error) {
                translating.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    /* ========== Rating ========== */

    public void rateComic(int comicId, int score) {
        rateLoading.setValue(true);
        comicRepository.rateComic(comicId, score, new ComicRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                rateLoading.postValue(false);
                rateMessage.postValue("SUCCESS");
                // Reload comic to get updated averageRating
                comicRepository.getComicById(comicId, new ComicRepository.ComicCallback() {
                    @Override
                    public void onSuccess(Comic refreshed) {
                        comic.postValue(refreshed);
                    }

                    @Override
                    public void onError(String error) {
                        // ignore refresh error, rating was still submitted
                    }
                });
            }

            @Override
            public void onError(String error) {
                rateLoading.postValue(false);
                rateMessage.postValue("ERROR:" + error);
            }
        });
    }

    /* ========== Follow ========== */

    public void loadFollowStatus(int comicId) {
        libraryRepository.getFollowStatus(comicId, new LibraryRepository.FollowStatusCallback() {
            @Override
            public void onSuccess(boolean isFollowed) {
                followed.postValue(isFollowed);
            }

            @Override
            public void onError(@NonNull String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void toggleFollow(int comicId) {
        followLoading.setValue(true);
        LibraryRepository.ActionCallback callback = new LibraryRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                boolean nowFollowed = !Boolean.TRUE.equals(followed.getValue());
                followed.postValue(nowFollowed);
                followLoading.postValue(false);
                followSuccessMessage.postValue(nowFollowed ? "Comic followed" : "Comic unfollowed");
            }

            @Override
            public void onError(@NonNull String message) {
                followLoading.postValue(false);
                errorMessage.postValue(message);
            }
        };

        if (Boolean.TRUE.equals(followed.getValue())) {
            libraryRepository.unfollowComic(comicId, callback);
        } else {
            libraryRepository.followComic(comicId, callback);
        }
    }

    /* ========== Getters ========== */

    public LiveData<Comic> getComic() { return comic; }
    public LiveData<List<Chapter>> getChapters() { return chapters; }
    public LiveData<Boolean> getChapterLoading() { return chapterLoading; }
    public LiveData<Boolean> getPurchaseLoading() { return purchaseLoading; }
    public LiveData<Chapter> getPurchasedChapter() { return purchasedChapter; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<Comic>> getRelatedComics() { return relatedComics; }
    public LiveData<Boolean> getFollowed() { return followed; }
    public LiveData<Boolean> getFollowLoading() { return followLoading; }
    public LiveData<String> getFollowSuccessMessage() { return followSuccessMessage; }
    public LiveData<String> getTranslatedTitle() { return translatedTitle; }
    public LiveData<String> getTranslatedSynopsis() { return translatedSynopsis; }
    public LiveData<Boolean> getTranslating() { return translating; }
    public LiveData<Boolean> getShowingTranslation() { return showingTranslation; }
    public LiveData<Boolean> getRateLoading() { return rateLoading; }
    public LiveData<String> getRateMessage() { return rateMessage; }
}
