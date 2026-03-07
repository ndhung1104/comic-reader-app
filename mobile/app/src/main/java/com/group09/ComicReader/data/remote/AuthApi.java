package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.AuthResponse;
import com.group09.ComicReader.model.LoginRequest;
import com.group09.ComicReader.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("/api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
}
