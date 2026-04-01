package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.ChangePasswordRequest;
import com.group09.ComicReader.model.UpdateProfileRequest;
import com.group09.ComicReader.model.UserProfileResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("/api/v1/users/me")
    Call<UserProfileResponse> getMe();

    @PUT("/api/v1/users/me")
    Call<UserProfileResponse> updateMe(@Body UpdateProfileRequest request);

    @PUT("/api/v1/users/me/password")
    Call<Map<String, Object>> changePassword(@Body ChangePasswordRequest request);

    @Multipart
    @PUT("/api/v1/users/me/avatar")
    Call<UserProfileResponse> updateAvatar(@Part MultipartBody.Part avatar);
}
