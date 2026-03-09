package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicChapterResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ComicApi {

    @GET("/api/v1/comics/{comicId}/chapters")
    Call<List<ComicChapterResponse>> getComicChapters(@Path("comicId") long comicId);
}
