package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class CommentSheetViewModel extends ViewModel {

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final MutableLiveData<List<CommentItem>> comments = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postSuccess = new MutableLiveData<>();
    private int currentComicId;

    public void loadComments(int comicId) {
        this.currentComicId = comicId;
        isLoading.setValue(true);
        comicRepository.getCommentsForComic(comicId, new ComicRepository.CommentListCallback() {
            @Override
            public void onSuccess(List<CommentItem> result) {
                isLoading.postValue(false);
                comments.postValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
                comments.postValue(new ArrayList<>());
            }
        });
    }

    public void addComment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        comicRepository.postComment(currentComicId, text.trim(), new ComicRepository.CommentCallback() {
            @Override
            public void onSuccess(CommentItem comment) {
                List<CommentItem> current = comments.getValue();
                List<CommentItem> updated = new ArrayList<>();
                updated.add(comment);
                if (current != null) {
                    updated.addAll(current);
                }
                comments.postValue(updated);
                postSuccess.postValue(true);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    public LiveData<List<CommentItem>> getComments() { return comments; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getPostSuccess() { return postSuccess; }
}
