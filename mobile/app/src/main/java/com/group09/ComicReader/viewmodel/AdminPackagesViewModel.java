package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.group09.ComicReader.data.AdminRepository;

import java.util.List;
import java.util.Map;

public class AdminPackagesViewModel extends ViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final AdminRepository repository;

        public Factory(AdminRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AdminPackagesViewModel.class)) {
                return (T) new AdminPackagesViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final AdminRepository repository;
    private final MutableLiveData<List<Map<String, Object>>> packagesList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public AdminPackagesViewModel(AdminRepository repository) {
        this.repository = repository;
    }

    public void loadPackages() {
        loading.setValue(true);
        repository.getAllPackages(new AdminRepository.PackageListCallback() {
            @Override
            public void onSuccess(@NonNull List<Map<String, Object>> packages) {
                packagesList.postValue(packages);
                loading.postValue(false);
            }

            @Override
            public void onError(@NonNull String errorMsg) {
                message.postValue(errorMsg);
                loading.postValue(false);
            }
        });
    }

    public void createPackage(String name, int coins, String priceLabel, String bonusLabel, int sortOrder) {
        loading.setValue(true);
        repository.createPackage(name, coins, priceLabel, bonusLabel, sortOrder, new AdminRepository.PackageActionCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> pkg) {
                message.postValue("Package created successfully");
                loadPackages();
            }

            @Override
            public void onError(@NonNull String errorMsg) {
                message.postValue(errorMsg);
                loading.postValue(false);
            }
        });
    }

    public void togglePackageStatus(long id, boolean isCurrentlyActive) {
        loading.setValue(true);
        AdminRepository.PackageActionCallback callback = new AdminRepository.PackageActionCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> pkg) {
                message.postValue("Status updated successfully");
                loadPackages();
            }

            @Override
            public void onError(@NonNull String errorMsg) {
                message.postValue(errorMsg);
                loading.postValue(false);
            }
        };

        if (isCurrentlyActive) {
            repository.disablePackage(id, callback);
        } else {
            repository.enablePackage(id, callback);
        }
    }

    public LiveData<List<Map<String, Object>>> getPackagesList() { return packagesList; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }
}
