package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.AiSummaryResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AiApi {

    @POST("/api/v1/ai/chat")
    Call<com.group09.ComicReader.model.AiChatResponse> chat(@Body Map<String, Object> body);

    @POST("/api/v1/ai/summary/generate")
    Call<AiSummaryResponse> generateSummary(@Body Map<String, Object> body);

    @POST("/api/v1/ai/summary/{id}/moderate")
    Call<AiSummaryResponse> moderateSummary(
            @Path("id") long id,
            @Query("status") String status,
            @Query("reason") String reason
    );

    @GET("/api/v1/ai/summary/history")
    Call<List<AiSummaryResponse>> getHistory(
            @Query("comicId") long comicId,
            @Query("chapterId") Long chapterId
    );

    @GET("/api/v1/ai/summary/latest")
    Call<AiSummaryResponse> getLatestApproved(
            @Query("comicId") long comicId,
            @Query("chapterId") Long chapterId
    );
}
