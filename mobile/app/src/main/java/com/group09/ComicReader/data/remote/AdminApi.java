package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.UserProfileResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AdminApi {

    @PUT("/api/v1/admin/users/{userId}/ban")
    Call<UserProfileResponse> banUser(@Path("userId") long userId);

    @PUT("/api/v1/admin/users/{userId}/unban")
    Call<UserProfileResponse> unbanUser(@Path("userId") long userId);

    @PUT("/api/v1/admin/comments/{commentId}/hide")
    Call<CommentItem> hideComment(@Path("commentId") long commentId);

    @PUT("/api/v1/admin/comments/{commentId}/unhide")
    Call<CommentItem> unhideComment(@Path("commentId") long commentId);

    @PUT("/api/v1/admin/comments/{commentId}/lock")
    Call<CommentItem> lockComment(@Path("commentId") long commentId);

    @PUT("/api/v1/admin/comments/{commentId}/unlock")
    Call<CommentItem> unlockComment(@Path("commentId") long commentId);

    @DELETE("/api/v1/admin/comments/{commentId}")
    Call<Void> deleteComment(@Path("commentId") long commentId);
}
