package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.WalletRepository;
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
    private final MutableLiveData<Integer> balance = new MutableLiveData<>();
    private final MutableLiveData<List<WalletPackage>> packages = new MutableLiveData<>();
    private final MutableLiveData<List<WalletTransaction>> transactions = new MutableLiveData<>();
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
            public void onSuccess(int newBalance, @NonNull List<WalletPackage> walletPackages,
                                  @NonNull List<WalletTransaction> walletTransactions) {
                loading.postValue(false);
                balance.postValue(newBalance);
                packages.postValue(walletPackages);
                transactions.postValue(walletTransactions);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void topUp(@NonNull WalletPackage walletPackage) {
        toppingUp.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);
        walletRepository.topUp(walletPackage, new WalletRepository.WalletTopUpCallback() {
            @Override
            public void onSuccess(int newBalance) {
                toppingUp.postValue(false);
                balance.postValue(newBalance);
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

    public LiveData<Integer> getBalance() { return balance; }
    public LiveData<List<WalletPackage>> getPackages() { return packages; }
    public LiveData<List<WalletTransaction>> getTransactions() { return transactions; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getToppingUp() { return toppingUp; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
}
