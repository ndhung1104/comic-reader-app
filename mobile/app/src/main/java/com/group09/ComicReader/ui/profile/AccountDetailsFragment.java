package com.group09.ComicReader.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAccountDetailsBinding;
import com.group09.ComicReader.model.UserProfileResponse;

public class AccountDetailsFragment extends BaseFragment {

    private FragmentAccountDetailsBinding binding;
    private SessionManager sessionManager;
    private AccountRepository accountRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        accountRepository = new AccountRepository(new ApiClient(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAccountDetailsBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        if (!sessionManager.hasToken()) {
            Navigation.findNavController(view).navigate(R.id.loginFragment);
            return;
        }

        bindInitialValues();
        fetchMeIfNeeded();

        binding.btnAccountDetailsSaveName.setOnClickListener(v -> submitNameUpdate());
        binding.btnAccountDetailsUpdatePassword.setOnClickListener(v -> submitPasswordChange());
    }

    private void bindInitialValues() {
        String fullName = sessionManager.getFullName();
        if (fullName != null) {
            binding.edtAccountDetailsFullName.setText(fullName);
        }
    }

    private void fetchMeIfNeeded() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            return;
        }

        setLoading(true);
        accountRepository.getMe(new AccountRepository.MeCallback() {
            @Override
            public void onSuccess(@NonNull UserProfileResponse me) {
                setLoading(false);
                if (me.getFullName() != null) {
                    sessionManager.saveFullName(me.getFullName());
                    binding.edtAccountDetailsFullName.setText(me.getFullName());
                }
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
            }
        });
    }

    private void submitNameUpdate() {
        String fullName = binding.edtAccountDetailsFullName.getText() == null
                ? ""
                : binding.edtAccountDetailsFullName.getText().toString().trim();

        if (fullName.isEmpty()) {
            showToast(getString(R.string.profile_full_name) + " is required");
            return;
        }

        hideKeyboard();
        setLoading(true);
        accountRepository.updateFullName(fullName, new AccountRepository.MeCallback() {
            @Override
            public void onSuccess(@NonNull UserProfileResponse me) {
                setLoading(false);
                if (me.getFullName() != null) {
                    sessionManager.saveFullName(me.getFullName());
                    binding.edtAccountDetailsFullName.setText(me.getFullName());
                }
                showToast("Name updated");
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void submitPasswordChange() {
        String currentPassword = binding.edtAccountDetailsCurrentPassword.getText() == null
                ? ""
                : binding.edtAccountDetailsCurrentPassword.getText().toString();
        String newPassword = binding.edtAccountDetailsNewPassword.getText() == null
                ? ""
                : binding.edtAccountDetailsNewPassword.getText().toString();
        String confirmPassword = binding.edtAccountDetailsConfirmPassword.getText() == null
                ? ""
                : binding.edtAccountDetailsConfirmPassword.getText().toString();

        if (currentPassword.trim().isEmpty() || newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            showToast("Please fill in all password fields");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showToast("New password and confirmation do not match");
            return;
        }

        hideKeyboard();
        setLoading(true);
        accountRepository.changePassword(currentPassword, newPassword, new AccountRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                binding.edtAccountDetailsCurrentPassword.setText("");
                binding.edtAccountDetailsNewPassword.setText("");
                binding.edtAccountDetailsConfirmPassword.setText("");
                showToast("Password updated");
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnAccountDetailsSaveName.setEnabled(!isLoading);
        binding.btnAccountDetailsUpdatePassword.setEnabled(!isLoading);
        binding.pbAccountDetailsLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
