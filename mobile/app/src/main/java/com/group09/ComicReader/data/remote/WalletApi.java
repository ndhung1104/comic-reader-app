package com.group09.ComicReader.data.remote;

import com.group09.ComicReader.model.PageResponse;
import com.group09.ComicReader.model.TopUpRequest;
import com.group09.ComicReader.model.TransactionResponse;
import com.group09.ComicReader.model.WalletResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface WalletApi {

    @GET("/api/v1/wallet")
    Call<WalletResponse> getWallet();

    @GET("/api/v1/wallet/transactions")
    Call<PageResponse<TransactionResponse>> getTransactions(@Query("page") int page, @Query("size") int size);

    @POST("/api/v1/wallet/topup")
    Call<WalletResponse> topUp(@Body TopUpRequest request);

    @GET("/api/v1/packages")
    Call<java.util.List<com.group09.ComicReader.model.WalletPackage>> getPackages();
}
