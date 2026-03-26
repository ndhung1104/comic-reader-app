package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.CommentPageResponse;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.CreateCommentRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CommentApi {

    @GET("/api/v1/comics/{comicId}/comments")
    Call<List<CommentItem>> getComments(@Path("comicId") long comicId);

        @GET("/api/v1/comics/{comicId}/comments/paged")
        Call<CommentPageResponse> getCommentsPaged(
            @Path("comicId") long comicId,
            @Query("page") int page,
            @Query("size") int size,
            @Query("chapterId") Long chapterId);

    @POST("/api/v1/comics/{comicId}/comments")
    Call<CommentItem> postComment(@Path("comicId") long comicId, @Body CreateCommentRequest body);
}
