package com.group09.ComicReader.data.remote;

import android.content.Context;

import com.group09.ComicReader.data.local.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Emulator -> host machine
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/";

    private final Retrofit retrofit;

    public ApiClient(Context context) {
        SessionManager sessionManager = new SessionManager(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(sessionManager))
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public AuthApi authApi() {
        return retrofit.create(AuthApi.class);
    }

    public ComicApi comicApi() {
        return retrofit.create(ComicApi.class);
    }

    public ChapterApi chapterApi() {
        return retrofit.create(ChapterApi.class);
    }

    public static String toAbsoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return "";
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        String relative = pathOrUrl.startsWith("/") ? pathOrUrl.substring(1) : pathOrUrl;
        return DEFAULT_BASE_URL + relative;
    }
}
