package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.AdRewardRequest;
import com.group09.ComicReader.model.IapVerifyRequest;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.model.PageResponse;
import com.group09.ComicReader.model.TransactionResponse;
import com.group09.ComicReader.model.VipPurchaseRequest;
import com.group09.ComicReader.model.VipStatusResponse;
import com.group09.ComicReader.model.WalletTransaction;
import com.group09.ComicReader.model.WalletResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {

    public static final String REWARD_TYPE_COIN = "COIN";
    public static final String REWARD_TYPE_POINT = "POINT";

    public interface WalletDataCallback {
        void onSuccess(int coinBalance, int pointBalance, @NonNull List<WalletPackage> packages,
                       @NonNull List<WalletTransaction> transactions);

        void onError(@NonNull String message);
    }

    public interface WalletTopUpCallback {
        void onSuccess(int newCoinBalance, int newPointBalance);

        void onError(@NonNull String message);
    }

    public interface VipStatusCallback {
        void onSuccess(@NonNull VipStatusResponse vipStatusResponse);

        void onError(@NonNull String message);
    }


    private final ApiClient apiClient;

    public WalletRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void loadWalletData(@NonNull WalletDataCallback callback) {
        apiClient.walletApi().getWallet().enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletResponse> call, @NonNull Response<WalletResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load wallet"));
                    return;
                }

                WalletResponse wallet = response.body();
                int coinBalance = wallet.getCoinBalance() == null ? 0 : wallet.getCoinBalance();
                int pointBalance = wallet.getPointBalance() == null ? 0 : wallet.getPointBalance();
                loadPackages(coinBalance, pointBalance, callback);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void topUp(@NonNull WalletPackage walletPackage, @NonNull WalletTopUpCallback callback) {
        String purchaseToken = "sandbox-" + UUID.randomUUID();
        String orderId = "order-" + UUID.randomUUID();
        IapVerifyRequest request = new IapVerifyRequest(
                "GOOGLE",
                purchaseToken,
                walletPackage.getId(),
                walletPackage.getId() == null ? "coins_" + walletPackage.getCoins() : null,
                orderId
        );

        apiClient.walletApi().verifyIap(request).enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletResponse> call, @NonNull Response<WalletResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Top up verification failed"));
                    return;
                }

                Integer coinBalance = response.body().getCoinBalance();
                Integer pointBalance = response.body().getPointBalance();
                callback.onSuccess(coinBalance == null ? 0 : coinBalance, pointBalance == null ? 0 : pointBalance);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void claimAdReward(@NonNull String rewardType,
                              @NonNull String rewardId,
                              @NonNull String placement,
                              @NonNull WalletTopUpCallback callback) {
        AdRewardRequest request = new AdRewardRequest(
                "admob",
                com.group09.ComicReader.BuildConfig.ADMOB_REWARDED_UNIT_ID,
                rewardType,
                rewardId,
                placement
        );
        apiClient.walletApi().claimAdReward(request).enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletResponse> call, @NonNull Response<WalletResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Ad reward claim failed"));
                    return;
                }

                Integer coinBalance = response.body().getCoinBalance();
                Integer pointBalance = response.body().getPointBalance();
                callback.onSuccess(coinBalance == null ? 0 : coinBalance, pointBalance == null ? 0 : pointBalance);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void loadVipStatus(@NonNull VipStatusCallback callback) {
        apiClient.walletApi().getVipStatus().enqueue(new Callback<VipStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<VipStatusResponse> call, @NonNull Response<VipStatusResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load VIP status"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<VipStatusResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void purchaseVip(@NonNull String plan, @NonNull VipStatusCallback callback) {
        VipPurchaseRequest request = new VipPurchaseRequest(plan, REWARD_TYPE_COIN);
        apiClient.walletApi().purchaseVip(request).enqueue(new Callback<VipStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<VipStatusResponse> call, @NonNull Response<VipStatusResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "VIP purchase failed"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<VipStatusResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private void loadPackages(int coinBalance, int pointBalance, @NonNull WalletDataCallback callback) {
        apiClient.walletApi().getPackages().enqueue(new Callback<List<WalletPackage>>() {
            @Override
            public void onResponse(@NonNull Call<List<WalletPackage>> call, @NonNull Response<List<WalletPackage>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load packages"));
                    return;
                }
                loadTransactions(coinBalance, pointBalance, response.body(), callback);
            }

            @Override
            public void onFailure(@NonNull Call<List<WalletPackage>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private void loadTransactions(int coinBalance, int pointBalance, List<WalletPackage> packages, @NonNull WalletDataCallback callback) {
        apiClient.walletApi().getTransactions(0, 20).enqueue(new Callback<PageResponse<TransactionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<TransactionResponse>> call,
                                   @NonNull Response<PageResponse<TransactionResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load transactions"));
                    return;
                }

                List<WalletTransaction> transactions = new ArrayList<>();
                List<TransactionResponse> content = response.body().getContent();
                if (content != null) {
                    for (TransactionResponse item : content) {
                        transactions.add(toWalletTransaction(item));
                    }
                }
                callback.onSuccess(coinBalance, pointBalance, packages, transactions);
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<TransactionResponse>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    @NonNull
    private WalletTransaction toWalletTransaction(@NonNull TransactionResponse item) {
        long id = item.getId() == null ? 0L : item.getId();
        int amount = item.getAmount() == null ? 0 : item.getAmount();
        String rawType = item.getType() == null ? "TRANSACTION" : item.getType();
        String type = resolveTransactionTitle(item);
        if (isDebitType(rawType) && amount > 0) {
            amount = -amount;
        }
        String date = item.getCreatedAt() == null ? "" : item.getCreatedAt().replace('T', ' ');
        return new WalletTransaction(id, type, amount, date);
    }

    @NonNull
    private String resolveTransactionTitle(@NonNull TransactionResponse item) {
        String description = item.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return item.getType() == null ? "TRANSACTION" : item.getType();
    }

    private boolean isDebitType(@NonNull String type) {
        String value = type.toUpperCase(Locale.US);
        return "PURCHASE".equals(value) || "VIP_PURCHASE".equals(value) || "REFUND".equals(value);
    }

    @NonNull
    private String getNetworkMessage(@NonNull Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()
                ? "Network error"
                : throwable.getMessage();
    }

    @NonNull
    private String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return fallback;
            }
            JSONObject json = new JSONObject(raw);
            String message = json.optString("error", json.optString("message", fallback));
            return message == null || message.trim().isEmpty() ? fallback : message;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
