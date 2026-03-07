package com.group09.ComicReader.ui.login;

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
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentLoginBinding;
import com.group09.ComicReader.viewmodel.LoginViewModel;

public class LoginFragment extends BaseFragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        SessionManager sessionManager = new SessionManager(requireContext());
        AuthRepository authRepository = new AuthRepository(apiClient, sessionManager);
        viewModel = new ViewModelProvider(this, new LoginViewModel.Factory(authRepository)).get(LoginViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (viewModel.hasToken()) {
            Navigation.findNavController(view).navigate(LoginFragmentDirections.actionLoginToHome());
            return;
        }

        View.OnClickListener loginAction = v -> {
            hideKeyboard();
            String email = binding.edtLoginEmail.getText() == null ? "" : binding.edtLoginEmail.getText().toString();
            String password = binding.edtLoginPassword.getText() == null ? "" : binding.edtLoginPassword.getText().toString();
            viewModel.login(email, password);
        };

        binding.btnLoginSubmit.setOnClickListener(loginAction);
        binding.btnLoginGoogle.setOnClickListener(v -> showToast("Google login is not implemented"));
        binding.tvLoginForgot.setOnClickListener(v -> showToast("Forgot password is not implemented"));

        binding.tvLoginSignUp.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(LoginFragmentDirections.actionLoginToRegister()));

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = isLoading != null && isLoading;
            binding.btnLoginSubmit.setEnabled(!loading);
            binding.btnLoginGoogle.setEnabled(!loading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });

        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Navigation.findNavController(view).navigate(LoginFragmentDirections.actionLoginToHome());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
