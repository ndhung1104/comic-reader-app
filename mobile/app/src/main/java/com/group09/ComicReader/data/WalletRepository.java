package com.group09.ComicReader.data;

import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.model.WalletTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WalletRepository {

    private static WalletRepository instance;

    public static WalletRepository getInstance() {
        if (instance == null) {
            instance = new WalletRepository();
        }
        return instance;
    }

    public int getBalance() {
        return 1500;
    }

    public List<WalletPackage> getPackages() {
        return new ArrayList<>(Arrays.asList(
                new WalletPackage(500, "$4.99", ""),
                new WalletPackage(1000, "$9.99", "+100 Bonus"),
                new WalletPackage(2500, "$19.99", "+500 Bonus"),
                new WalletPackage(5000, "$39.99", "+1000 Bonus")
        ));
    }

    public List<WalletTransaction> getTransactions() {
        return new ArrayList<>(Arrays.asList(
                new WalletTransaction(1, "purchase", 1000, "Mar 1, 2026"),
                new WalletTransaction(2, "spent", -50, "Feb 28, 2026"),
                new WalletTransaction(3, "reward", 200, "Feb 27, 2026"),
                new WalletTransaction(4, "spent", -30, "Feb 26, 2026")
        ));
    }
}
