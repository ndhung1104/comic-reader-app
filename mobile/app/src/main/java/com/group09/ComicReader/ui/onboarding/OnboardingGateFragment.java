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

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentOnboardingGateBinding;

public class OnboardingGateFragment extends BaseFragment {

    private FragmentOnboardingGateBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingGateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            return;
        }

        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.onboardingGateFragment) {
            return;
        }

        NavOptions replaceSelf = new NavOptions.Builder()
                .setPopUpTo(R.id.onboardingGateFragment, true)
                .build();

        SessionManager sessionManager = new SessionManager(requireContext());
        if (sessionManager.hasToken()) {
            navController.navigate(R.id.homeFragment, null, replaceSelf);
            return;
        }

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        if (settings.isOnboardingCompleted()) {
            navController.navigate(R.id.homeFragment, null, replaceSelf);
            return;
        }

        int step = settings.getOnboardingStep();
        if (step == 1) {
            navController.navigate(R.id.onboardingDobFragment, null, replaceSelf);
            return;
        }
        if (step == 2) {
            navController.navigate(R.id.onboardingInterestsFragment, null, replaceSelf);
            return;
        }
        navController.navigate(R.id.onboardingLanguageFragment, null, replaceSelf);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
