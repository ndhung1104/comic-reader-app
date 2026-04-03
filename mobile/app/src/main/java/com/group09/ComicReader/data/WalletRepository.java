package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.model.PageResponse;
import com.group09.ComicReader.model.TopUpRequest;
import com.group09.ComicReader.model.TransactionResponse;
import com.group09.ComicReader.model.WalletTransaction;
import com.group09.ComicReader.model.WalletResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {

    public interface WalletDataCallback {
        void onSuccess(int balance, @NonNull List<WalletPackage> packages,
                       @NonNull List<WalletTransaction> transactions);

        void onError(@NonNull String message);
    }

    public interface WalletTopUpCallback {
        void onSuccess(int newBalance);

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
                int balance = wallet.getCoinBalance() == null ? 0 : wallet.getCoinBalance();
                loadPackages(balance, callback);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    public void topUp(@NonNull WalletPackage walletPackage, @NonNull WalletTopUpCallback callback) {
        TopUpRequest request = new TopUpRequest(
                walletPackage.getCoins(),
                "COIN",
                "wallet-package-" + walletPackage.getCoins()
        );

        apiClient.walletApi().topUp(request).enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletResponse> call, @NonNull Response<WalletResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Top up failed"));
                    return;
                }

                Integer coinBalance = response.body().getCoinBalance();
                callback.onSuccess(coinBalance == null ? 0 : coinBalance);
            }

            @Override
            public void onFailure(@NonNull Call<WalletResponse> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private void loadPackages(int balance, @NonNull WalletDataCallback callback) {
        apiClient.walletApi().getPackages().enqueue(new Callback<List<WalletPackage>>() {
            @Override
            public void onResponse(@NonNull Call<List<WalletPackage>> call, @NonNull Response<List<WalletPackage>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load packages"));
                    return;
                }
                loadTransactions(balance, response.body(), callback);
            }

            @Override
            public void onFailure(@NonNull Call<List<WalletPackage>> call, @NonNull Throwable t) {
                callback.onError(getNetworkMessage(t));
            }
        });
    }

    private void loadTransactions(int balance, List<WalletPackage> packages, @NonNull WalletDataCallback callback) {
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
                callback.onSuccess(balance, packages, transactions);
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
        String type = item.getType() == null ? "TRANSACTION" : item.getType();
        if (isDebitType(type) && amount > 0) {
            amount = -amount;
        }
        String date = item.getCreatedAt() == null ? "" : item.getCreatedAt().replace('T', ' ');
        return new WalletTransaction(id, type, amount, date);
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
