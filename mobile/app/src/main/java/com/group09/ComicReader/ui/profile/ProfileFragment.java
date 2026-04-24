package com.group09.ComicReader.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ProfileMenuAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.WalletRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentProfileBinding;
import com.group09.ComicReader.model.CreatorRequestResponse;
import com.group09.ComicReader.model.ProfileMenuItem;
import com.group09.ComicReader.model.UpdateUserPreferencesRequest;
import com.group09.ComicReader.model.UserProfileResponse;
import com.group09.ComicReader.model.VipStatusResponse;
import com.group09.ComicReader.viewmodel.ProfileViewModel;
import com.group09.ComicReader.data.CreatorRepository;
import androidx.appcompat.app.AlertDialog;

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
    private CreatorRepository creatorRepository;
    private WalletRepository walletRepository;
    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        sessionManager = new SessionManager(requireContext());
        accountRepository = new AccountRepository(new ApiClient(requireContext()));
        creatorRepository = new CreatorRepository(new ApiClient(requireContext()));
        walletRepository = new WalletRepository(new ApiClient(requireContext()));

        pickAvatarLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null)
                return;
            if (!sessionManager.hasToken()) {
                return;
            }
            uploadAvatar(uri);
        });
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

        binding.imgProfileAvatar.setOnClickListener(v -> {
            if (!sessionManager.hasToken()) {
                Navigation.findNavController(v).navigate(R.id.loginFragment);
                return;
            }
            pickAvatarLauncher.launch("image/*");
        });
        binding.tvProfileMembership.setOnClickListener(v -> {
            if (!sessionManager.hasToken()) {
                Navigation.findNavController(v).navigate(R.id.loginFragment);
                return;
            }
            Navigation.findNavController(v).navigate(R.id.walletFragment);
        });

        renderForAuthState();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(sessionManager.getRole());
        boolean isCreator = "CREATOR".equalsIgnoreCase(sessionManager.getRole());
        viewModel.loadData(isAdmin, isCreator, requireContext());
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
            if (email == null)
                email = "";
            if (name == null || name.trim().isEmpty()) {
                name = email.trim().isEmpty() ? getString(R.string.profile_guest_name) : email;
            }
            viewModel.setUserInfo(name, email);

            renderAvatarFromSession();

            maybeFetchMe();
            refreshMembershipStatus();

            binding.btnProfileCreator.setVisibility(View.VISIBLE);
            String role = sessionManager.getRole();
            if ("CREATOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) {
                binding.btnProfileCreator.setText(R.string.profile_creator_already);
                binding.btnProfileCreator.setEnabled(false);
                binding.btnProfileCreator.setAlpha(0.5f);
            } else {
                binding.btnProfileCreator.setEnabled(true);
                binding.btnProfileCreator.setAlpha(1.0f);
                syncCreatorButton();
            }

            setAuthButtonToLogout(binding.btnProfileLogout);
        } else {
            viewModel.setUserInfo(getString(R.string.profile_guest_name), getString(R.string.profile_not_signed_in));
            binding.btnProfileCreator.setVisibility(View.GONE);
            binding.tvProfileMembership.setText(R.string.profile_membership);
            clearAvatar();
            setAuthButtonToLogin(binding.btnProfileLogout);
        }
    }

    private void syncCreatorButton() {
        binding.btnProfileCreator.setText(R.string.profile_creator);
        binding.btnProfileCreator.setOnClickListener(v -> showBecomeCreatorDialog());

        creatorRepository.getMyRequest(new CreatorRepository.CreatorRequestCallback() {
            @Override
            public void onSuccess(@NonNull CreatorRequestResponse response) {
                if (binding == null) return;
                String status = response.getStatus();
                if ("PENDING".equalsIgnoreCase(status)) {
                    binding.btnProfileCreator.setText(R.string.profile_creator_pending_title);
                    binding.btnProfileCreator.setOnClickListener(v -> showPendingDialog());
                } else if ("DENIED".equalsIgnoreCase(status)) {
                    binding.btnProfileCreator.setText(R.string.profile_creator_denied_title);
                    binding.btnProfileCreator.setOnClickListener(v -> showDeniedDialog(response.getAdminMessage()));
                } else if ("APPROVED".equalsIgnoreCase(status)) {
                    // This should ideally lead to role update, but for now disable it
                    binding.btnProfileCreator.setText(R.string.profile_creator_already);
                    binding.btnProfileCreator.setEnabled(false);
                    binding.btnProfileCreator.setAlpha(0.5f);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                // Not found or network error, keep "Become a Creator" state
            }
        });
    }

    private void refreshMembershipStatus() {
        walletRepository.loadVipStatus(new WalletRepository.VipStatusCallback() {
            @Override
            public void onSuccess(@NonNull VipStatusResponse vipStatusResponse) {
                if (binding == null) {
                    return;
                }
                if (!vipStatusResponse.isVip()) {
                    binding.tvProfileMembership.setText(R.string.profile_membership);
                    return;
                }

                String plan = vipStatusResponse.getPlan();
                if ("YEARLY".equalsIgnoreCase(plan)) {
                    binding.tvProfileMembership.setText(R.string.wallet_vip_plan_yearly);
                } else {
                    binding.tvProfileMembership.setText(R.string.wallet_vip_plan_monthly);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (binding != null) {
                    binding.tvProfileMembership.setText(R.string.profile_membership);
                }
            }
        });
    }

    private void showBecomeCreatorDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_creator_dialog_title)
                .setMessage(R.string.profile_creator_dialog_message)
                .setNegativeButton(R.string.profile_creator_cancel, null)
                .setPositiveButton(R.string.profile_creator_send_request, (dialog, which) -> sendCreatorRequest())
                .show();
    }

    private void showPendingDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_creator_pending_title)
                .setMessage(R.string.profile_creator_pending_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showDeniedDialog(String adminMessage) {
        String message = getString(R.string.profile_creator_denied_message, adminMessage != null ? adminMessage : "No reason provided");
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_creator_denied_title)
                .setMessage(message)
                .setNegativeButton(R.string.profile_creator_cancel, null)
                .setPositiveButton(R.string.profile_creator_resend, (dialog, which) -> showBecomeCreatorDialog())
                .show();
    }

    private void sendCreatorRequest() {
        creatorRepository.requestCreator("I want to become a creator", new CreatorRepository.CreatorRequestCallback() {
            @Override
            public void onSuccess(@NonNull CreatorRequestResponse response) {
                showToast("Request sent successfully");
                syncCreatorButton();
            }

            @Override
            public void onError(@NonNull String message) {
                showToast(message);
            }
        });
    }

    private void maybeFetchMe() {
        String currentName = sessionManager.getFullName();
        String currentEmail = sessionManager.getEmail();
        String currentAvatar = sessionManager.getAvatarUrl();

        boolean needsName = currentName == null || currentName.trim().isEmpty();
        boolean needsEmail = currentEmail == null || currentEmail.trim().isEmpty();
        boolean needsAvatar = currentAvatar == null || currentAvatar.trim().isEmpty();
        if (!needsName && !needsEmail && !needsAvatar)
            return;

        accountRepository.getMe(new AccountRepository.MeCallback() {
            @Override
            public void onSuccess(@NonNull UserProfileResponse me) {
                if (binding == null)
                    return;
                if (me.getEmail() != null && !me.getEmail().trim().isEmpty()) {
                    sessionManager.saveEmail(me.getEmail());
                }
                if (me.getFullName() != null && !me.getFullName().trim().isEmpty()) {
                    sessionManager.saveFullName(me.getFullName());
                }
                if (me.getAvatarUrl() != null && !me.getAvatarUrl().trim().isEmpty()) {
                    sessionManager.saveAvatarUrl(me.getAvatarUrl());
                }
                String name = sessionManager.getFullName();
                if (name == null || name.trim().isEmpty())
                    name = getString(R.string.profile_guest_name);
                String email = sessionManager.getEmail();
                if (email == null)
                    email = "";
                viewModel.setUserInfo(name, email);

                renderAvatarFromSession();
            }

            @Override
            public void onError(@NonNull String message) {
                // silent fail: keep current UI
            }
        });
    }

    private void uploadAvatar(@NonNull Uri uri) {
        if (binding == null)
            return;

        Glide.with(binding.imgProfileAvatar)
                .load(uri)
                .placeholder(R.drawable.ic_avatar_placeholder_24)
                .error(R.drawable.ic_avatar_placeholder_24)
                .circleCrop()
                .into(binding.imgProfileAvatar);

        accountRepository.updateAvatar(requireContext(), uri, new AccountRepository.MeCallback() {
            @Override
            public void onSuccess(@NonNull UserProfileResponse me) {
                if (binding == null)
                    return;
                if (me.getAvatarUrl() != null && !me.getAvatarUrl().trim().isEmpty()) {
                    sessionManager.saveAvatarUrl(me.getAvatarUrl());
                }
                renderAvatarFromSession();
            }

            @Override
            public void onError(@NonNull String message) {
                if (binding == null)
                    return;
                showToast(message);
                renderAvatarFromSession();
            }
        });
    }

    private void renderAvatarFromSession() {
        if (binding == null)
            return;
        String avatarUrl = sessionManager.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            clearAvatar();
            return;
        }

        Glide.with(binding.imgProfileAvatar)
                .load(ApiClient.toAbsoluteUrl(avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder_24)
                .error(R.drawable.ic_avatar_placeholder_24)
                .circleCrop()
                .into(binding.imgProfileAvatar);
    }

    private void clearAvatar() {
        if (binding == null)
            return;
        Glide.with(binding.imgProfileAvatar).clear(binding.imgProfileAvatar);
        binding.imgProfileAvatar.setImageResource(R.drawable.ic_avatar_placeholder_24);
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
        if (item == null)
            return;
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
        if ("ADMIN_CREATOR_REQUESTS".equals(item.getType()) && getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.adminCreatorRequestsFragment);
            return;
        }
        if ("CREATOR_STUDIO".equals(item.getType()) && getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.creatorStudioFragment);
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
        if ("INTERESTS".equals(item.getType()) && getView() != null) {
            NavDirections action = ProfileFragmentDirections.actionProfileToEditInterests();
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

                if (sessionManager.hasToken()) {
                    accountRepository.updateMyPreferences(
                            new UpdateUserPreferencesRequest(code, null, null),
                            new AccountRepository.PreferencesCallback() {
                                @Override
                                public void onSuccess(
                                        @NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
                                    // no-op
                                }

                                @Override
                                public void onError(@NonNull String message) {
                                    if (binding == null)
                                        return;
                                    showToast(message);
                                }
                            });
                }

                // reload menu to update badge
                boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole())
                        || "ROLE_ADMIN".equalsIgnoreCase(sessionManager.getRole());
                boolean isCreator = "CREATOR".equalsIgnoreCase(sessionManager.getRole());
                viewModel.loadData(isAdmin, isCreator, requireContext());
            }, currentLang);
            return;
        }
        if ("DARK_MODE".equals(item.getType())) {
            AppSettingsStore settings = new AppSettingsStore(requireContext());
            boolean nextDarkMode = !settings.isDarkModeEnabled();
            settings.setDarkModeEnabled(nextDarkMode);
            AppCompatDelegate.setDefaultNightMode(nextDarkMode
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole())
                    || "ROLE_ADMIN".equalsIgnoreCase(sessionManager.getRole());
            boolean isCreator = "CREATOR".equalsIgnoreCase(sessionManager.getRole());
            viewModel.loadData(isAdmin, isCreator, requireContext());
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
