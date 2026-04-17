package com.group09.ComicReader.ui.register;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AuthRepository;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentRegisterBinding;
import com.group09.ComicReader.model.UpdateUserPreferencesRequest;
import com.group09.ComicReader.viewmodel.RegisterViewModel;

import com.group09.ComicReader.ui.common.LanguagePickerDialog;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AppCompatDelegate;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Set;

public class RegisterFragment extends BaseFragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Nullable
    private String selectedDobIso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        ApiClient apiClient = new ApiClient(requireContext());
        SessionManager sessionManager = new SessionManager(requireContext());
        AuthRepository authRepository = new AuthRepository(apiClient, sessionManager);
        viewModel = new ViewModelProvider(this, new RegisterViewModel.Factory(authRepository)).get(RegisterViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show language picker if not selected yet
        AppSettingsStore appSettingsStore = new AppSettingsStore(requireContext());
        if (!appSettingsStore.isLanguageSelected()) {
            LanguagePickerDialog.show(requireContext(), code -> {
                appSettingsStore.setLanguageCode(code);
                appSettingsStore.setLanguageSelected(true);
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
            });
        }

        selectedDobIso = appSettingsStore.getBirthDateIso();
        if (selectedDobIso != null && !selectedDobIso.trim().isEmpty()) {
            binding.edtRegisterDob.setText(selectedDobIso);
        }

        View.OnClickListener dobPicker = v -> {
            hideKeyboard();
            showDobPicker(appSettingsStore);
        };
        binding.tilRegisterDob.setOnClickListener(dobPicker);
        binding.edtRegisterDob.setOnClickListener(dobPicker);

        binding.btnRegisterSubmit.setOnClickListener(v -> {
            hideKeyboard();
            String fullName = binding.edtRegisterFullName.getText() == null ? "" : binding.edtRegisterFullName.getText().toString();
            String email = binding.edtRegisterEmail.getText() == null ? "" : binding.edtRegisterEmail.getText().toString();
            String password = binding.edtRegisterPassword.getText() == null ? "" : binding.edtRegisterPassword.getText().toString();
            String confirmPassword = binding.edtRegisterConfirmPassword.getText() == null ? "" : binding.edtRegisterConfirmPassword.getText().toString();
            viewModel.register(email, password, confirmPassword, fullName, selectedDobIso);
        });

        binding.btnRegisterTabSignup.setOnClickListener(v -> {
            // Current screen.
        });
        binding.btnRegisterTabLogin.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(RegisterFragmentDirections.actionRegisterToLogin()));
        binding.tvRegisterLogin.setOnClickListener(v -> Navigation.findNavController(v).navigate(RegisterFragmentDirections.actionRegisterToLogin()));

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = isLoading != null && isLoading;
            binding.btnRegisterSubmit.setEnabled(!loading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });

        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) {
                return;
            }

            syncPreferencesToServerIfLoggedIn();

            AppSettingsStore settings = new AppSettingsStore(requireContext());
            boolean shouldCollectGenres = !settings.isOnboardingCompleted()
                    || settings.getPreferredGenres().size() < 3;
            if (shouldCollectGenres) {
                settings.setOnboardingStep(2);
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.registerFragment, true)
                        .build();
                Navigation.findNavController(view).navigate(R.id.onboardingInterestsFragment, null, options);
                return;
            }

            Navigation.findNavController(view).navigate(RegisterFragmentDirections.actionRegisterToHome());
        });
    }

    private void showDobPicker(@NonNull AppSettingsStore settings) {
        LocalDate initial = LocalDate.of(2000, 1, 1);
        try {
            String iso = selectedDobIso;
            if (iso != null && !iso.trim().isEmpty()) {
                initial = LocalDate.parse(iso.trim());
            }
        } catch (Exception ignored) {
            // fallback to default
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, monthOfYear, dayOfMonth) -> {
                    LocalDate dob = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
                    selectedDobIso = dob.toString();
                    binding.edtRegisterDob.setText(selectedDobIso);

                    settings.setBirthDateIso(selectedDobIso);
                    int age = Period.between(dob, LocalDate.now()).getYears();
                    settings.setAllowMatureContent(age >= 18);
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void syncPreferencesToServerIfLoggedIn() {
        if (!isAdded()) return;

        SessionManager sessionManager = new SessionManager(requireContext());
        if (!sessionManager.hasToken()) return;

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        String languageCode = settings.getLanguageCode();
        String dobIso = settings.getBirthDateIso();
        Set<String> genres = settings.getPreferredGenres();

        java.util.List<String> preferredGenres = null;
        if (genres != null && genres.size() >= 3) {
            preferredGenres = new ArrayList<>(genres);
        }

        ApiClient apiClient = new ApiClient(requireContext());
        AccountRepository accountRepository = new AccountRepository(apiClient);
        accountRepository.updateMyPreferences(
                new UpdateUserPreferencesRequest(languageCode, dobIso, preferredGenres),
                new AccountRepository.PreferencesCallback() {
                    @Override
                    public void onSuccess(@NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
                        // no-op
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        // Best-effort; ignore to avoid blocking registration flow.
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
