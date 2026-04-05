package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.ReaderPageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ChapterApi {

    @GET("/api/v1/chapters/{chapterId}")
    Call<ComicChapterResponse> getChapter(@Path("chapterId") long chapterId);

    @GET("/api/v1/chapters/{chapterId}/pages")
    Call<List<ReaderPageResponse>> getChapterPages(@Path("chapterId") long chapterId);
}
