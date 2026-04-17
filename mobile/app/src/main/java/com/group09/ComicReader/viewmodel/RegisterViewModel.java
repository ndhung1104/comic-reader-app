package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.AuthRepository;

import java.time.LocalDate;

public class RegisterViewModel extends ViewModel {

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final AuthRepository authRepository;

        public Factory(@NonNull AuthRepository authRepository) {
            this.authRepository = authRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
                return (T) new RegisterViewModel(authRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>(false);

    public RegisterViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getRegisterSuccess() {
        return registerSuccess;
    }

    public void register(String email, String password, String confirmPassword, String fullName, String dateOfBirthIso) {
        String safeEmail = email == null ? "" : email.trim();
        String safePassword = password == null ? "" : password.trim();
        String safeConfirmPassword = confirmPassword == null ? "" : confirmPassword.trim();
        String safeFullName = fullName == null ? "" : fullName.trim();
        String safeDob = dateOfBirthIso == null ? "" : dateOfBirthIso.trim();

        if (safeFullName.isEmpty()) {
            errorMessage.setValue("Full name is required");
            return;
        }
        if (safeEmail.isEmpty()) {
            errorMessage.setValue("Email is required");
            return;
        }
        if (safePassword.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters");
            return;
        }
        if (!safePassword.equals(safeConfirmPassword)) {
            errorMessage.setValue("Passwords do not match");
            return;
        }

        if (safeDob.isEmpty()) {
            errorMessage.setValue("Date of birth is required");
            return;
        }

        try {
            LocalDate.parse(safeDob);
        } catch (Exception ignored) {
            errorMessage.setValue("Invalid date of birth");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);
        registerSuccess.setValue(false);

        authRepository.register(safeEmail, safePassword, safeFullName, safeDob, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull com.group09.ComicReader.model.AuthResponse authResponse) {
                loading.postValue(false);
                registerSuccess.postValue(true);
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
