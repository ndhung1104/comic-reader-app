package com.group09.ComicReader.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AuthRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentRegisterBinding;
import com.group09.ComicReader.viewmodel.RegisterViewModel;

import com.group09.ComicReader.ui.common.LanguagePickerDialog;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AppCompatDelegate;

public class RegisterFragment extends BaseFragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

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

        binding.btnRegisterSubmit.setOnClickListener(v -> {
            hideKeyboard();
            String fullName = binding.edtRegisterFullName.getText() == null ? "" : binding.edtRegisterFullName.getText().toString();
            String email = binding.edtRegisterEmail.getText() == null ? "" : binding.edtRegisterEmail.getText().toString();
            String password = binding.edtRegisterPassword.getText() == null ? "" : binding.edtRegisterPassword.getText().toString();
            String confirmPassword = binding.edtRegisterConfirmPassword.getText() == null ? "" : binding.edtRegisterConfirmPassword.getText().toString();
            viewModel.register(email, password, confirmPassword, fullName);
        });

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
            if (success != null && success) {
                Navigation.findNavController(view).navigate(RegisterFragmentDirections.actionRegisterToHome());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
