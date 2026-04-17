package com.group09.ComicReader.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.databinding.FragmentOnboardingLanguageBinding;

import java.util.Locale;

public class OnboardingLanguageFragment extends BaseFragment {

    private FragmentOnboardingLanguageBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        String current = settings.getLanguageCode();
        if (current == null || current.trim().isEmpty()) {
            String deviceLang = Locale.getDefault().getLanguage();
            current = "vi".equalsIgnoreCase(deviceLang) ? "vi" : "en";
        }

        binding.btnLanguageVi.setChecked("vi".equalsIgnoreCase(current));
        binding.btnLanguageEn.setChecked("en".equalsIgnoreCase(current));

        binding.btnOnboardingContinue.setOnClickListener(v -> {
            int checkedId = binding.tgOnboardingLanguage.getCheckedButtonId();
            String code = checkedId == R.id.btn_language_vi ? "vi" : "en";

            settings.setLanguageCode(code);
            settings.setLanguageSelected(true);
            settings.setOnboardingStep(1);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.onboardingDobFragment);

            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
