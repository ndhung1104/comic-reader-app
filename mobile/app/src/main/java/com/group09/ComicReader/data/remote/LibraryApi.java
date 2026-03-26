package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.FollowStatusResponse;
import com.group09.ComicReader.model.FollowedComicResponse;
import com.group09.ComicReader.model.ReadingHistoryRequest;
import com.group09.ComicReader.model.RecentReadResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface LibraryApi {

    @GET("/api/v1/library/followed")
    Call<List<FollowedComicResponse>> getFollowedComics();

    @GET("/api/v1/library/recent")
    Call<List<RecentReadResponse>> getRecentReads();

    @GET("/api/v1/library/followed/{comicId}/status")
    Call<FollowStatusResponse> getFollowStatus(@Path("comicId") long comicId);

    @POST("/api/v1/library/followed/{comicId}")
    Call<FollowStatusResponse> followComic(@Path("comicId") long comicId);

    @DELETE("/api/v1/library/followed/{comicId}")
    Call<FollowStatusResponse> unfollowComic(@Path("comicId") long comicId);

    @POST("/api/v1/library/history")
    Call<RecentReadResponse> recordReadingHistory(@Body ReadingHistoryRequest request);
}
