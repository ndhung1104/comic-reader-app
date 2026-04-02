package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.AdminRepository;

import java.util.List;
import java.util.Map;

public class AdminRevenueViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final AdminRepository repository;

        public Factory(AdminRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AdminRevenueViewModel.class)) {
                return (T) new AdminRevenueViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final AdminRepository repository;
    private final MutableLiveData<Long> totalTopUp = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalPurchase = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalVip = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalRevenue = new MutableLiveData<>(0L);
    private final MutableLiveData<List<Map<String, Object>>> dailyList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AdminRevenueViewModel(AdminRepository repository) {
        this.repository = repository;
    }

    public void loadData(String from, String to) {
        loading.setValue(true);
        error.setValue(null);
        repository.getRevenueSummary(from, to, new AdminRepository.RevenueSummaryCallback() {
            @Override
            public void onSuccess(long topUp, long purchase, long vip, long total, long txCount) {
                totalTopUp.postValue(topUp);
                totalPurchase.postValue(purchase);
                totalVip.postValue(vip);
                totalRevenue.postValue(total);
                
                // Now load daily breakdown
                repository.getDailyRevenue(from, to, new AdminRepository.DailyRevenueCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Map<String, Object>> dailyData) {
                        dailyList.postValue(dailyData);
                        loading.postValue(false);
                    }
                    @Override
                    public void onError(@NonNull String message) {
                        error.postValue(message);
                        loading.postValue(false);
                    }
                });
            }

            @Override
            public void onError(@NonNull String message) {
                error.postValue(message);
                loading.postValue(false);
            }
        });
    }

    public LiveData<Long> getTotalTopUp() { return totalTopUp; }
    public LiveData<Long> getTotalPurchase() { return totalPurchase; }
    public LiveData<Long> getTotalVip() { return totalVip; }
    public LiveData<Long> getTotalRevenue() { return totalRevenue; }
    public LiveData<List<Map<String, Object>>> getDailyList() { return dailyList; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
}
