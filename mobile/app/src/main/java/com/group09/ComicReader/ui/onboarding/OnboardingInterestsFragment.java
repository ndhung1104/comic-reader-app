package com.group09.ComicReader.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AccountRepository;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentOnboardingInterestsBinding;
import com.group09.ComicReader.model.UpdateUserPreferencesRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingInterestsFragment extends BaseFragment {

    private static final int MIN_GENRES = 3;

    private static final String[] DEFAULT_ART_STYLES = new String[]{
            "Manga",
            "Manhwa",
            "Comic",
            "Chibi",
            "Realistic"
    };

    private FragmentOnboardingInterestsBinding binding;

    private final ComicRepository comicRepository = ComicRepository.getInstance();
    private final Set<String> selectedGenres = new HashSet<>();
    private final Set<String> selectedArtStyles = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingInterestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        selectedGenres.clear();
        selectedGenres.addAll(settings.getPreferredGenres());
        selectedArtStyles.clear();
        selectedArtStyles.addAll(settings.getPreferredArtStyles());

        setupArtStyleChips();
        loadGenreChips();

        binding.btnOnboardingFinish.setOnClickListener(v -> {
            if (selectedGenres.size() < MIN_GENRES) {
                showToast(getString(R.string.onboarding_genres_min_required, MIN_GENRES));
                return;
            }

            settings.setPreferredGenres(selectedGenres);
            settings.setPreferredArtStyles(selectedArtStyles);
            settings.setOnboardingCompleted(true);
            settings.setOnboardingStep(0);

            syncPreferencesToServerIfLoggedIn(settings);

            NavController navController = NavHostFragment.findNavController(this);
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.onboardingInterestsFragment, true)
                    .build();
            navController.navigate(R.id.homeFragment, null, options);
        });

        updateSelectionUi();
    }

    private void syncPreferencesToServerIfLoggedIn(@NonNull AppSettingsStore settings) {
        if (!isAdded()) return;

        SessionManager sessionManager = new SessionManager(requireContext());
        if (!sessionManager.hasToken()) return;

        String languageCode = settings.getLanguageCode();
        String dobIso = settings.getBirthDateIso();
        java.util.List<String> preferredGenres = new java.util.ArrayList<>(selectedGenres);

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
                        // Best-effort.
                    }
                }
        );
    }

    private void setupArtStyleChips() {
        binding.cgOnboardingArtStyles.removeAllViews();

        for (String style : DEFAULT_ART_STYLES) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_onboarding_chip, binding.cgOnboardingArtStyles, false);
            chip.setText(style);
            chip.setCheckable(true);
            chip.setChecked(selectedArtStyles.contains(style));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedArtStyles.add(style);
                } else {
                    selectedArtStyles.remove(style);
                }
            });
            binding.cgOnboardingArtStyles.addView(chip);
        }
    }

    private void loadGenreChips() {
        binding.cgOnboardingGenres.removeAllViews();
        binding.prgOnboardingGenresLoading.setVisibility(View.VISIBLE);

        comicRepository.getFilters(new ComicRepository.CategoryListCallback() {
            @Override
            public void onSuccess(List<String> categories) {
                if (!isAdded()) {
                    return;
                }
                binding.prgOnboardingGenresLoading.setVisibility(View.GONE);
                if (categories == null || categories.isEmpty()) {
                    showToast(getString(R.string.onboarding_genres_load_failed));
                    return;
                }

                for (String raw : categories) {
                    if (raw == null) {
                        continue;
                    }
                    String category = raw.trim();
                    if (category.isEmpty()) {
                        continue;
                    }
                    if ("all".equalsIgnoreCase(category)) {
                        continue;
                    }

                    Chip chip = (Chip) LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_onboarding_chip, binding.cgOnboardingGenres, false);
                    chip.setText(category);
                    chip.setCheckable(true);
                    chip.setChecked(selectedGenres.contains(category));
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedGenres.add(category);
                        } else {
                            selectedGenres.remove(category);
                        }
                        updateSelectionUi();
                    });
                    binding.cgOnboardingGenres.addView(chip);
                }

                updateSelectionUi();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                binding.prgOnboardingGenresLoading.setVisibility(View.GONE);
                showToast(error == null || error.trim().isEmpty()
                        ? getString(R.string.onboarding_genres_load_failed)
                        : error);
            }
        });
    }

    private void updateSelectionUi() {
        int count = selectedGenres.size();
        binding.tvOnboardingGenreCount.setText(getString(R.string.onboarding_genres_selected_count, count));
        binding.btnOnboardingFinish.setEnabled(count >= MIN_GENRES);

        if (count < MIN_GENRES) {
            binding.tvOnboardingGenreHint.setText(getString(R.string.onboarding_genres_min_required, MIN_GENRES));
        } else {
            binding.tvOnboardingGenreHint.setText(getString(R.string.onboarding_genres_ready));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
