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
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View.OnClickListener loginAction = v -> {
            hideKeyboard();
            String email = binding.edtLoginEmail.getText() == null ? "" : binding.edtLoginEmail.getText().toString();
            String password = binding.edtLoginPassword.getText() == null ? "" : binding.edtLoginPassword.getText().toString();
            if (viewModel.login(email, password)) {
                Navigation.findNavController(v).navigate(LoginFragmentDirections.actionLoginToHome());
            }
        };

        binding.btnLoginSubmit.setOnClickListener(loginAction);
        binding.btnLoginGoogle.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(LoginFragmentDirections.actionLoginToHome()));
        binding.tvLoginForgot.setOnClickListener(v -> showToast("Forgot password is not implemented"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
