package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.CommentItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CommentApi {

    @GET("/api/v1/comics/{comicId}/comments")
    Call<List<CommentItem>> getComments(@Path("comicId") long comicId);

    @POST("/api/v1/comics/{comicId}/comments")
    Call<CommentItem> postComment(@Path("comicId") long comicId, @Body Map<String, String> body);
}
