package com.group09.ComicReader.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.model.UpdateUserPreferencesRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EditInterestsViewModel extends ViewModel {

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final AppSettingsStore settings;
        private final SessionManager sessionManager;
        private final AccountRepository accountRepository;

        public Factory(
                @NonNull AppSettingsStore settings,
                @NonNull SessionManager sessionManager,
                @NonNull AccountRepository accountRepository
        ) {
            this.settings = settings;
            this.sessionManager = sessionManager;
            this.accountRepository = accountRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(EditInterestsViewModel.class)) {
                return (T) new EditInterestsViewModel(settings, sessionManager, accountRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private final AppSettingsStore settings;
    private final SessionManager sessionManager;
    private final AccountRepository accountRepository;

    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private EditInterestsViewModel(
            @NonNull AppSettingsStore settings,
            @NonNull SessionManager sessionManager,
            @NonNull AccountRepository accountRepository
    ) {
        this.settings = settings;
        this.sessionManager = sessionManager;
        this.accountRepository = accountRepository;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void saveAndSync(@NonNull Set<String> genres, @NonNull Set<String> artStyles) {
        settings.setPreferredGenres(genres);
        settings.setPreferredArtStyles(artStyles);

        saved.setValue(true);

        if (!sessionManager.hasToken()) {
            return;
        }

        String languageCode = settings.getLanguageCode();
        String dobIso = settings.getBirthDateIso();

        List<String> preferredGenres = new ArrayList<>(genres);
        accountRepository.updateMyPreferences(
                new UpdateUserPreferencesRequest(languageCode, dobIso, preferredGenres),
                new AccountRepository.PreferencesCallback() {
                    @Override
                    public void onSuccess(@NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
                        // no-op
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        errorMessage.postValue(message);
                    }
                }
        );
    }
}
