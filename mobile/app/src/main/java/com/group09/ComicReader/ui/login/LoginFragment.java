package com.group09.ComicReader.ui.login;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.group09.ComicReader.BuildConfig;
import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.AuthRepository;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentLoginBinding;
import com.group09.ComicReader.model.UpdateUserPreferencesRequest;
import com.group09.ComicReader.viewmodel.LoginViewModel;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.security.MessageDigest;
import java.util.Locale;

public class LoginFragment extends BaseFragment {

    private static final String TAG = "LoginFragment";

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel == null || !viewModel.hasToken()) return;
        androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == com.group09.ComicReader.R.id.loginFragment) {
            navController.navigate(LoginFragmentDirections.actionLoginToHome());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logDebugSigningSha1();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Google sign-in resultCode=" + result.getResultCode()
                            + ", hasData=" + (result.getData() != null));
                    Intent data = result.getData();
                    if (data == null) {
                        showToast("Google login cancelled");
                        return;
                    }
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);
                        if (account == null) {
                            Log.w(TAG, "GoogleSignInAccount is null");
                            showToast("Google login failed");
                            return;
                        }

                        String idToken = account.getIdToken();
                        if (idToken == null || idToken.trim().isEmpty()) {
                            Log.w(TAG, "Google idToken is empty; email=" + account.getEmail());
                            showToast("Google login failed (missing idToken)");
                            return;
                        }

                        String email = account.getEmail() == null ? "" : account.getEmail();
                        String fullName = account.getDisplayName() == null ? "" : account.getDisplayName();
                        viewModel.loginWithGoogle(idToken, email, fullName);
                    } catch (ApiException e) {
                        int statusCode = e.getStatusCode();
                        Log.w(TAG, "Google sign-in ApiException statusCode=" + statusCode
                                + " (" + GoogleSignInStatusCodes.getStatusCodeString(statusCode) + ")", e);
                        if (statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            showToast("Google login cancelled");
                        } else if (statusCode == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
                            // Common root cause: OAuth clients not configured correctly (package name/SHA-1)
                            // or using the wrong client id for requestIdToken().
                            showToast("Google login failed (developer error). Check OAuth config: package name + SHA-1, and ensure MOBILE_GOOGLE_WEB_CLIENT_ID is the Web client ID.");
                            logDebugSigningSha1();
                        } else {
                            showToast("Google login failed (" + statusCode + ": "
                                    + GoogleSignInStatusCodes.getStatusCodeString(statusCode) + ")");
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        SessionManager sessionManager = new SessionManager(requireContext());
        AuthRepository authRepository = new AuthRepository(apiClient, sessionManager);
        viewModel = new ViewModelProvider(this, new LoginViewModel.Factory(authRepository)).get(LoginViewModel.class);

        String webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID == null ? "" : BuildConfig.GOOGLE_WEB_CLIENT_ID.trim();
        if (BuildConfig.DEBUG) {
            String redactedClientId = webClientId.isEmpty() ? "" : (webClientId.length() <= 12
                ? webClientId
                : webClientId.substring(0, 6) + "..." + webClientId.substring(webClientId.length() - 6));
            Log.d(TAG, "Configured webClientId=" + redactedClientId);
        }
        if (!webClientId.isEmpty() && !webClientId.contains("<") && !webClientId.contains(">")) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(webClientId)
                    .build();
            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
        } else {
            googleSignInClient = null;
        }
        return binding.getRoot();
    }

    private void logDebugSigningSha1() {
        if (!BuildConfig.DEBUG) return;
        try {
            PackageManager pm = requireContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(
                    requireContext().getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES
            );

            if (packageInfo.signingInfo == null) return;

            Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
            if (signatures == null || signatures.length == 0) return;

            for (Signature signature : signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                byte[] digest = md.digest(signature.toByteArray());
                Log.d(TAG, "App signing SHA-1: " + toHexWithColons(digest)
                        + " (package=" + requireContext().getPackageName() + ")");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to compute app signing SHA-1", e);
        }
    }

    private String toHexWithColons(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format(Locale.US, "%02X", bytes[i]));
        }
        return sb.toString();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (viewModel.hasToken()) {
            androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == com.group09.ComicReader.R.id.loginFragment) {
                navController.navigate(LoginFragmentDirections.actionLoginToHome());
            }
            return;
        }

        View.OnClickListener loginAction = v -> {
            hideKeyboard();
            String email = binding.edtLoginEmail.getText() == null ? "" : binding.edtLoginEmail.getText().toString();
            String password = binding.edtLoginPassword.getText() == null ? "" : binding.edtLoginPassword.getText().toString();
            viewModel.login(email, password);
        };

        binding.btnLoginSubmit.setOnClickListener(loginAction);
        binding.btnLoginTabLogin.setOnClickListener(v -> {
            // Current screen.
        });
        binding.btnLoginTabSignup.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(LoginFragmentDirections.actionLoginToRegister()));
        binding.btnLoginGoogle.setOnClickListener(v -> {
            hideKeyboard();
            if (googleSignInClient == null) {
                showToast("Google login is not configured");
                return;
            }

            int playServicesStatus = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(requireContext());
            if (playServicesStatus != ConnectionResult.SUCCESS) {
                showToast("Google Play services is not available: "
                    + GoogleApiAvailability.getInstance().getErrorString(playServicesStatus));
                return;
            }

            v.setEnabled(false);
            googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
                v.setEnabled(true);
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            });
        });
        binding.tvLoginForgot.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_login_to_forgot_password));

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
                syncPreferencesThenNavigateHome();
            }
        });
    }

    private void syncPreferencesThenNavigateHome() {
        if (!isAdded()) return;

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        settings.setOnboardingCompleted(true);
        settings.setOnboardingStep(0);

        ApiClient apiClient = new ApiClient(requireContext());
        AccountRepository accountRepository = new AccountRepository(apiClient);
        accountRepository.getMyPreferences(new AccountRepository.PreferencesCallback() {
            @Override
            public void onSuccess(@NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
                if (!isAdded()) return;

                boolean serverEmpty = isPreferencesEmpty(preferences);
                if (serverEmpty) {
                    pushLocalPreferences(settings, accountRepository, () -> navigateHomeSafely());
                    return;
                }

                applyServerPreferencesToLocal(preferences, settings);
                navigateHomeSafely();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                // Don't block login on sync failure.
                navigateHomeSafely();
            }
        });
    }

    private boolean isPreferencesEmpty(@NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
        String language = preferences.getLanguageCode();
        String dob = preferences.getDateOfBirth();
        List<String> genres = preferences.getPreferredGenres();

        boolean languageEmpty = language == null || language.trim().isEmpty();
        boolean dobEmpty = dob == null || dob.trim().isEmpty();
        boolean genresEmpty = genres == null || genres.isEmpty();

        return languageEmpty && dobEmpty && genresEmpty;
    }

    private void pushLocalPreferences(
            @NonNull AppSettingsStore settings,
            @NonNull AccountRepository accountRepository,
            @NonNull Runnable done
    ) {
        String languageCode = settings.getLanguageCode();
        String dobIso = settings.getBirthDateIso();
        Set<String> localGenres = settings.getPreferredGenres();

        List<String> preferredGenres = null;
        if (localGenres != null && localGenres.size() >= 3) {
            preferredGenres = new ArrayList<>(localGenres);
        }

        accountRepository.updateMyPreferences(
                new UpdateUserPreferencesRequest(languageCode, dobIso, preferredGenres),
                new AccountRepository.PreferencesCallback() {
                    @Override
                    public void onSuccess(@NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences) {
                        if (!isAdded()) return;
                        applyServerPreferencesToLocal(preferences, settings);
                        done.run();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        done.run();
                    }
                }
        );
    }

    private void applyServerPreferencesToLocal(
            @NonNull com.group09.ComicReader.model.UserPreferencesResponse preferences,
            @NonNull AppSettingsStore settings
    ) {
        String languageCode = preferences.getLanguageCode();
        if (languageCode != null && !languageCode.trim().isEmpty()) {
            String safe = languageCode.trim();
            settings.setLanguageCode(safe);
            settings.setLanguageSelected(true);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(safe));
        }

        String dobIso = preferences.getDateOfBirth();
        if (dobIso != null && !dobIso.trim().isEmpty()) {
            String safeDob = dobIso.trim();
            settings.setBirthDateIso(safeDob);
            try {
                LocalDate dob = LocalDate.parse(safeDob);
                int age = Period.between(dob, LocalDate.now()).getYears();
                settings.setAllowMatureContent(age >= 18);
            } catch (Exception ignored) {
                // keep stored setting
            }
        }

        List<String> genres = preferences.getPreferredGenres();
        if (genres != null && genres.size() >= 3) {
            Set<String> asSet = new HashSet<>();
            for (String raw : genres) {
                if (raw == null) continue;
                String trimmed = raw.trim();
                if (!trimmed.isEmpty()) {
                    asSet.add(trimmed);
                }
            }
            if (asSet.size() >= 3) {
                settings.setPreferredGenres(asSet);
            }
        }
    }

    private void navigateHomeSafely() {
        if (!isAdded()) return;
        androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.loginFragment) {
            navController.navigate(LoginFragmentDirections.actionLoginToHome());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
