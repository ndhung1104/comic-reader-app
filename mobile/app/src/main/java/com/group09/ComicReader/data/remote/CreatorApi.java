package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.CreatorRequestResponse;
import com.group09.ComicReader.model.ImportJobResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CreatorApi {

    @POST("/api/v1/creator/comics/import")
    Call<ImportJobResponse> importComic(@Body Map<String, String> body);

    @POST("/api/v1/creator-requests")
    Call<CreatorRequestResponse> requestCreator(@Body Map<String, String> body);

    @GET("/api/v1/creator-requests/my")
    Call<CreatorRequestResponse> getMyRequest();

    @GET("/api/v1/creator/imports/{jobId}")
    Call<ImportJobResponse> getJob(@Path("jobId") long jobId);

    @GET("/api/v1/creator/imports")
    Call<Map<String, Object>> getMyJobs(@Query("page") int page, @Query("size") int size);

    @GET("/api/v1/creator/comics")
    Call<Map<String, Object>> getMyComics(@Query("page") int page, @Query("size") int size);

    @POST("/api/v1/creator/comics")
    Call<ComicResponse> createComic(@Body Map<String, Object> body);

    @DELETE("/api/v1/creator/comics/{id}")
    Call<Void> deleteComic(@Path("id") long id);
}
