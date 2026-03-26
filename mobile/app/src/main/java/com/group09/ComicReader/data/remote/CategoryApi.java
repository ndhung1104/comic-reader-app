package com.group09.ComicReader.data.remote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface CategoryApi {
    @GET("/api/v1/categories")
    Call<List<String>> getCategories();
}
