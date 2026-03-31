package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.PageResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
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

    @GET("/api/v1/comics/trending")
    Call<PageResponse<ComicResponse>> getTrending(
            @Query("page") Integer page,
            @Query("size") Integer size);

    @GET("/api/v1/comics/top-rated")
    Call<PageResponse<ComicResponse>> getTopRated(
            @Query("page") Integer page,
            @Query("size") Integer size);

    @GET("/api/v1/comics/{comicId}")
    Call<ComicResponse> getComic(@Path("comicId") long comicId);

    @GET("/api/v1/comics/{comicId}/chapters")
    Call<List<com.group09.ComicReader.model.ComicChapterResponse>> getComicChapters(@Path("comicId") long comicId);

    @GET("/api/v1/comics/{comicId}/related")
    Call<List<ComicResponse>> getRelatedComics(
            @Path("comicId") long comicId,
            @Query("size") int size);

    @POST("/api/v1/comics/{comicId}/rate")
    Call<Map<String, String>> rateComic(
            @Path("comicId") long comicId,
            @Body Map<String, Integer> body);

    @POST("/api/v1/comics/{comicId}/view")
    Call<Void> incrementView(@Path("comicId") long comicId);
}
