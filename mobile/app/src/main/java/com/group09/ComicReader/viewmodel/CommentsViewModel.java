package com.group09.ComicReader.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class CommentsViewModel extends ViewModel {

    private static final int DEFAULT_PAGE_SIZE = 15;

    private final ComicRepository comicRepository = ComicRepository.getInstance();

    private final MutableLiveData<List<CommentItem>> comments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> postSuccess = new MutableLiveData<>();

    private int comicId;
    @Nullable
    private Integer chapterId;
    private int currentPage = 0;

    public void init(int comicId, @Nullable Integer chapterId) {
        boolean changed = this.comicId != comicId
                || (this.chapterId == null && chapterId != null)
                || (this.chapterId != null && !this.chapterId.equals(chapterId));
        this.comicId = comicId;
        this.chapterId = chapterId;
        if (changed) {
            refresh();
        }
    }

    public void refresh() {
        currentPage = 0;
        comments.postValue(new ArrayList<>());
        loadPage(0);
    }

    public void loadMore() {
        Boolean loading = isLoading.getValue();
        if (loading != null && loading) return;

        Boolean more = hasMore.getValue();
        if (more == null || !more) return;

        loadPage(currentPage + 1);
    }

    public void addComment(String text) {
        if (text == null || text.trim().isEmpty()) return;

        comicRepository.postComment(comicId, chapterId, text.trim(), new ComicRepository.CommentCallback() {
            @Override
            public void onSuccess(CommentItem comment) {
                List<CommentItem> current = comments.getValue();
                List<CommentItem> updated = new ArrayList<>();
                updated.add(comment);
                if (current != null) updated.addAll(current);
                comments.postValue(updated);
                postSuccess.postValue(true);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    private void loadPage(int page) {
        isLoading.postValue(true);
        comicRepository.getCommentsForComicPaged(comicId, chapterId, page, DEFAULT_PAGE_SIZE,
                new ComicRepository.PagedCommentCallback() {
                    @Override
                    public void onSuccess(List<CommentItem> result, boolean more) {
                        isLoading.postValue(false);
                        currentPage = page;
                        hasMore.postValue(more);

                        List<CommentItem> current = comments.getValue();
                        List<CommentItem> updated = new ArrayList<>();
                        if (current != null && page > 0) {
                            updated.addAll(current);
                        }
                        if (result != null) {
                            updated.addAll(result);
                        }
                        comments.postValue(updated);
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.postValue(false);
                        errorMessage.postValue(error);
                        if (page == 0) {
                            comments.postValue(new ArrayList<>());
                        }
                    }
                });
    }

    public LiveData<List<CommentItem>> getComments() {
        return comments;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getPostSuccess() {
        return postSuccess;
    }
}
