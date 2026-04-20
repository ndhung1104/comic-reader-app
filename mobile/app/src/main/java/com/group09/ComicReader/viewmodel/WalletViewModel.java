package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.WalletRepository;
import com.group09.ComicReader.model.VipStatusResponse;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.model.WalletTransaction;

import java.util.List;

public class WalletViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final WalletRepository walletRepository;

        public Factory(@NonNull WalletRepository walletRepository) {
            this.walletRepository = walletRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(WalletViewModel.class)) {
                return (T) new WalletViewModel(walletRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final WalletRepository walletRepository;
    private final MutableLiveData<Integer> coinBalance = new MutableLiveData<>();
    private final MutableLiveData<Integer> pointBalance = new MutableLiveData<>();
    private final MutableLiveData<List<WalletPackage>> packages = new MutableLiveData<>();
    private final MutableLiveData<List<WalletTransaction>> transactions = new MutableLiveData<>();
    private final MutableLiveData<VipStatusResponse> vipStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> toppingUp = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>(null);

    public WalletViewModel(@NonNull WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public void loadData() {
        loading.setValue(true);
        errorMessage.setValue(null);
        walletRepository.loadWalletData(new WalletRepository.WalletDataCallback() {
            @Override
            public void onSuccess(int newCoinBalance, int newPointBalance, @NonNull List<WalletPackage> walletPackages,
                                  @NonNull List<WalletTransaction> walletTransactions) {
                loading.postValue(false);
                coinBalance.postValue(newCoinBalance);
                pointBalance.postValue(newPointBalance);
                packages.postValue(walletPackages);
                transactions.postValue(walletTransactions);
                loadVipStatus(false);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void topUp(@NonNull WalletPackage walletPackage) {
        if (Boolean.TRUE.equals(toppingUp.getValue()) || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        toppingUp.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        walletRepository.topUp(walletPackage, new WalletRepository.WalletTopUpCallback() {
            @Override
            public void onSuccess(int newCoinBalance, int newPointBalance) {
                toppingUp.postValue(false);
                coinBalance.postValue(newCoinBalance);
                pointBalance.postValue(newPointBalance);
                successMessage.postValue("Top up successful");
                loadData();
            }

            @Override
            public void onError(@NonNull String message) {
                toppingUp.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void claimAdReward(@NonNull String rewardType) {
        if (Boolean.TRUE.equals(toppingUp.getValue()) || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        toppingUp.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        walletRepository.claimAdReward(rewardType, java.util.UUID.randomUUID().toString(), "wallet",
                new WalletRepository.WalletTopUpCallback() {
                    @Override
                    public void onSuccess(int newCoinBalance, int newPointBalance) {
                        toppingUp.postValue(false);
                        coinBalance.postValue(newCoinBalance);
                        pointBalance.postValue(newPointBalance);
                        successMessage.postValue(WalletRepository.REWARD_TYPE_POINT.equals(rewardType)
                                ? "Ad reward added to your points"
                                : "Ad reward added to your coins");
                        loadData();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        toppingUp.postValue(false);
                        errorMessage.postValue(message);
                    }
                });
    }

    public void purchaseVip(@NonNull String plan) {
        if (Boolean.TRUE.equals(toppingUp.getValue()) || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        toppingUp.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        walletRepository.purchaseVip(plan, new WalletRepository.VipStatusCallback() {
            @Override
            public void onSuccess(@NonNull VipStatusResponse vipStatusResponse) {
                toppingUp.postValue(false);
                vipStatus.postValue(vipStatusResponse);
                successMessage.postValue("VIP activated successfully");
                loadData();
            }

            @Override
            public void onError(@NonNull String message) {
                toppingUp.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void loadVipStatus(boolean forceLoadingState) {
        if (forceLoadingState) {
            loading.setValue(true);
        }
        walletRepository.loadVipStatus(new WalletRepository.VipStatusCallback() {
            @Override
            public void onSuccess(@NonNull VipStatusResponse vipStatusResponse) {
                vipStatus.postValue(vipStatusResponse);
                if (forceLoadingState) {
                    loading.postValue(false);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (forceLoadingState) {
                    loading.postValue(false);
                }
                errorMessage.postValue(message);
            }
        });
    }

    public LiveData<Integer> getCoinBalance() { return coinBalance; }
    public LiveData<Integer> getPointBalance() { return pointBalance; }
    public LiveData<List<WalletPackage>> getPackages() { return packages; }
    public LiveData<List<WalletTransaction>> getTransactions() { return transactions; }
    public LiveData<VipStatusResponse> getVipStatus() { return vipStatus; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getToppingUp() { return toppingUp; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
}
