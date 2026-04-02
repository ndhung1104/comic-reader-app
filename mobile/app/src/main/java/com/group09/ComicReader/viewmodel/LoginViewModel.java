package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.AuthRepository;

public class LoginViewModel extends ViewModel {

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final AuthRepository authRepository;

        public Factory(@NonNull AuthRepository authRepository) {
            this.authRepository = authRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(LoginViewModel.class)) {
                return (T) new LoginViewModel(authRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public boolean hasToken() {
        return authRepository.hasToken();
    }

    public void login(String email, String password) {
        String safeEmail = email == null ? "" : email.trim();
        String safePassword = password == null ? "" : password.trim();

        if (safeEmail.isEmpty()) {
            errorMessage.setValue("Email is required");
            return;
        }
        if (safePassword.isEmpty()) {
            errorMessage.setValue("Password is required");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);
        loginSuccess.setValue(false);

        authRepository.login(safeEmail, safePassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull com.group09.ComicReader.model.AuthResponse authResponse) {
                loading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void loginWithGoogle(String idToken, String email, String fullName) {
        String safeIdToken = idToken == null ? "" : idToken.trim();
        if (safeIdToken.isEmpty()) {
            errorMessage.setValue("Google login failed (missing token)");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);
        loginSuccess.setValue(false);

        String safeEmail = email == null ? "" : email.trim();
        String safeName = fullName == null ? "" : fullName.trim();

        authRepository.loginWithGoogle(safeIdToken, safeEmail, safeName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull com.group09.ComicReader.model.AuthResponse authResponse) {
                loading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
