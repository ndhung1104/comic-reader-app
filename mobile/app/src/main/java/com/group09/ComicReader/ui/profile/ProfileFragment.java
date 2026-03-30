package com.group09.ComicReader.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ProfileMenuAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentProfileBinding;
import com.group09.ComicReader.model.ProfileMenuItem;
import com.group09.ComicReader.model.UserProfileResponse;
import com.group09.ComicReader.viewmodel.ProfileViewModel;

import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.ui.common.LanguagePickerDialog;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends BaseFragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private ProfileMenuAdapter adapter;
    private SessionManager sessionManager;
    private AccountRepository accountRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        sessionManager = new SessionManager(requireContext());
        accountRepository = new AccountRepository(new ApiClient(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new ProfileMenuAdapter(this::onMenuClicked);
        binding.rcvProfileMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvProfileMenu.setAdapter(adapter);

        viewModel.getUsername().observe(getViewLifecycleOwner(), name -> binding.tvProfileName.setText(name));
        viewModel.getEmail().observe(getViewLifecycleOwner(), email -> binding.tvProfileEmail.setText(email));
        viewModel.getMenuItems().observe(getViewLifecycleOwner(), adapter::submitList);

        binding.btnProfileCreator.setOnClickListener(v -> showToast("Creator flow is not implemented"));

        renderForAuthState();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(sessionManager.getRole());
        viewModel.loadData(isAdmin, requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        renderForAuthState();
    }

    private void renderForAuthState() {
        boolean isLoggedIn = sessionManager.hasToken();

        if (isLoggedIn) {
            String name = sessionManager.getFullName();
            String email = sessionManager.getEmail();
            if (email == null) email = "";
            if (name == null || name.trim().isEmpty()) {
                name = email.trim().isEmpty() ? getString(R.string.profile_guest_name) : email;
            }
            viewModel.setUserInfo(name, email);

            maybeFetchMe();

            binding.btnProfileCreator.setVisibility(View.VISIBLE);
            setAuthButtonToLogout(binding.btnProfileLogout);
        } else {
            viewModel.setUserInfo(getString(R.string.profile_guest_name), getString(R.string.profile_not_signed_in));
            binding.btnProfileCreator.setVisibility(View.GONE);
            setAuthButtonToLogin(binding.btnProfileLogout);
        }
    }

    private void maybeFetchMe() {
        String currentName = sessionManager.getFullName();
        String currentEmail = sessionManager.getEmail();

        boolean needsName = currentName == null || currentName.trim().isEmpty();
        boolean needsEmail = currentEmail == null || currentEmail.trim().isEmpty();
        if (!needsName && !needsEmail) return;

        accountRepository.getMe(new AccountRepository.MeCallback() {
            @Override
            public void onSuccess(@NonNull UserProfileResponse me) {
                if (me.getEmail() != null && !me.getEmail().trim().isEmpty()) {
                    sessionManager.saveEmail(me.getEmail());
                }
                if (me.getFullName() != null && !me.getFullName().trim().isEmpty()) {
                    sessionManager.saveFullName(me.getFullName());
                }
                String name = sessionManager.getFullName();
                if (name == null || name.trim().isEmpty()) name = getString(R.string.profile_guest_name);
                String email = sessionManager.getEmail();
                if (email == null) email = "";
                viewModel.setUserInfo(name, email);
            }

            @Override
            public void onError(@NonNull String message) {
                // silent fail: keep current UI
            }
        });
    }

    private void setAuthButtonToLogout(@NonNull MaterialButton button) {
        button.setText(R.string.profile_logout);
        button.setStrokeColorResource(R.color.danger_color);
        button.setOnClickListener(v -> {
            sessionManager.clear();
            androidx.navigation.NavController navController = Navigation.findNavController(v);
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true)
                    .build();
            navController.navigate(R.id.loginFragment, null, navOptions);
        });
    }

    private void setAuthButtonToLogin(@NonNull MaterialButton button) {
        button.setText(R.string.login_button);
        button.setStrokeColorResource(R.color.accent_primary);
        button.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.loginFragment));
    }

    private void onMenuClicked(ProfileMenuItem item) {
        if (item == null) return;
        if ("ACCOUNT_DETAILS".equals(item.getType()) && getView() != null) {
            if (!sessionManager.hasToken()) {
                Navigation.findNavController(getView()).navigate(R.id.loginFragment);
                return;
            }
            Navigation.findNavController(getView()).navigate(R.id.accountDetailsFragment);
            return;
        }
        if ("ADMIN_DASHBOARD".equals(item.getType()) && getView() != null) {
            NavDirections action = ProfileFragmentDirections.actionProfileToAdminDashboard();
            Navigation.findNavController(getView()).navigate(action);
            return;
        }
        if (item.isNavigatesToWallet() && getView() != null) {
            if (!sessionManager.hasToken()) {
                Navigation.findNavController(getView()).navigate(R.id.loginFragment);
                return;
            }
            NavDirections action = ProfileFragmentDirections.actionProfileToWallet();
            Navigation.findNavController(getView()).navigate(action);
            return;
        }
        if ("LANGUAGE".equals(item.getType())) {
            AppSettingsStore settings = new AppSettingsStore(requireContext());
            String currentLang = settings.getLanguageCode();
            LanguagePickerDialog.show(requireContext(), code -> {
                settings.setLanguageCode(code);
                settings.setLanguageSelected(true);
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
                // reload menu to update badge
                boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(sessionManager.getRole());
                viewModel.loadData(isAdmin, requireContext());
            }, currentLang);
            return;
        }
        showToast(item.getLabel());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
