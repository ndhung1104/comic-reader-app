package com.group09.ComicReader.model;

import com.google.gson.annotations.SerializedName;

public class CreateCommentRequest {

    @SerializedName("content")
    private final String content;

    @SerializedName("sourceType")
    private final String sourceType;

    @SerializedName("chapterId")
    private final Long chapterId;

    public CreateCommentRequest(String content, String sourceType, Long chapterId) {
        this.content = content;
        this.sourceType = sourceType;
        this.chapterId = chapterId;
    }
}
