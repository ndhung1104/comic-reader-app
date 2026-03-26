package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CommentPageResponse {

    @SerializedName("content")
    private List<CommentItem> content;

    @SerializedName("last")
    private boolean last;

    @SerializedName("number")
    private int number;

    @SerializedName("size")
    private int size;

    public List<CommentItem> getContent() {
        return content;
    }

    public boolean isLast() {
        return last;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }
}
