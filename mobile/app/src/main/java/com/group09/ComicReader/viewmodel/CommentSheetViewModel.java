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

    public void loadComments(int comicId) {
        comments.setValue(comicRepository.getCommentsForComic(comicId));
    }

    public void addComment(String text) {
        if (text == null || text.trim().isEmpty() || comments.getValue() == null) {
            return;
        }
        List<CommentItem> current = new ArrayList<>(comments.getValue());
        int nextId = current.size() + 1;
        current.add(0, new CommentItem(nextId, "You", "", text.trim(), "Just now", 0));
        comments.setValue(current);
    }

    public LiveData<List<CommentItem>> getComments() { return comments; }
}
