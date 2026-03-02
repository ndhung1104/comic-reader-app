package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.WalletRepository;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.model.WalletTransaction;

import java.util.List;

public class WalletViewModel extends ViewModel {

    private final WalletRepository walletRepository = WalletRepository.getInstance();
    private final MutableLiveData<Integer> balance = new MutableLiveData<>();
    private final MutableLiveData<List<WalletPackage>> packages = new MutableLiveData<>();
    private final MutableLiveData<List<WalletTransaction>> transactions = new MutableLiveData<>();

    public void loadData() {
        balance.setValue(walletRepository.getBalance());
        packages.setValue(walletRepository.getPackages());
        transactions.setValue(walletRepository.getTransactions());
    }

    public LiveData<Integer> getBalance() { return balance; }
    public LiveData<List<WalletPackage>> getPackages() { return packages; }
    public LiveData<List<WalletTransaction>> getTransactions() { return transactions; }
}
