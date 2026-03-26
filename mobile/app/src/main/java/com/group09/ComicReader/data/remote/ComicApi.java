package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.PageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ComicApi {

    @GET("/api/v1/comics")
    Call<PageResponse<ComicResponse>> getComics(
            @Query("keyword") String keyword,
            @Query("categoryId") String categoryId,
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort);

    @GET("/api/v1/comics/{comicId}")
    Call<ComicResponse> getComic(@Path("comicId") long comicId);

    @GET("/api/v1/comics/{comicId}/chapters")
    Call<List<ComicChapterResponse>> getComicChapters(@Path("comicId") long comicId);
}
