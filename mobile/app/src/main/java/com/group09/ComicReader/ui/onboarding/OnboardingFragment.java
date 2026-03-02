package com.group09.ComicReader.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentOnboardingBinding;
import com.group09.ComicReader.viewmodel.OnboardingViewModel;

public class OnboardingFragment extends BaseFragment {

    private FragmentOnboardingBinding binding;
    private OnboardingViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnOnboardingGetStarted.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(OnboardingFragmentDirections.actionOnboardingToLogin()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
