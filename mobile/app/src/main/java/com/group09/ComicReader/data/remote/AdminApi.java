package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.UserProfileResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Field;

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

    // ── Revenue ──────────────────────────────────────────

    @GET("/api/v1/admin/revenue/summary")
    Call<Map<String, Object>> getRevenueSummary(@Query("from") String from, @Query("to") String to);

    @GET("/api/v1/admin/revenue/daily")
    Call<List<Map<String, Object>>> getDailyRevenue(@Query("from") String from, @Query("to") String to);

    // ── Packages ─────────────────────────────────────────

    @GET("/api/v1/admin/packages")
    Call<List<Map<String, Object>>> getAllPackages();

    @POST("/api/v1/admin/packages")
    Call<Map<String, Object>> createPackage(@Body Map<String, Object> body);

    @PUT("/api/v1/admin/packages/{id}")
    Call<Map<String, Object>> updatePackage(@Path("id") long id, @Body Map<String, Object> body);

    @PUT("/api/v1/admin/packages/{id}/disable")
    Call<Map<String, Object>> disablePackage(@Path("id") long id);

    @PUT("/api/v1/admin/packages/{id}/enable")
    Call<Map<String, Object>> enablePackage(@Path("id") long id);

    // ── Import ───────────────────────────────────────────

    @POST("/api/v1/admin/comics/import")
    Call<ComicResponse> importComic(@Body Map<String, String> body);

    // ── Creator Requests ─────────────────────────────────

    @GET("/api/v1/admin/creator-requests")
    Call<com.group09.ComicReader.model.CreatorRequestPageResponse> getCreatorRequests(@Query("page") int page,
            @Query("size") int size);

    @FormUrlEncoded
    @POST("/api/v1/admin/creator-requests/{id}/approve")
    Call<com.group09.ComicReader.model.CreatorRequestResponse> approveCreatorRequest(@Path("id") long id,
            @Field("adminMessage") String adminMessage);

    @FormUrlEncoded
    @POST("/api/v1/admin/creator-requests/{id}/deny")
    Call<com.group09.ComicReader.model.CreatorRequestResponse> denyCreatorRequest(@Path("id") long id,
            @Field("adminMessage") String adminMessage);
}
