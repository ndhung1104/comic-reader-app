package com.group09.ComicReader.ui.onboarding;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.databinding.FragmentOnboardingDobBinding;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OnboardingDobFragment extends BaseFragment {

    private FragmentOnboardingDobBinding binding;
    @Nullable
    private LocalDate selectedDob;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingDobBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppSettingsStore settings = new AppSettingsStore(requireContext());
        String existing = settings.getBirthDateIso();
        if (existing != null && !existing.trim().isEmpty()) {
            try {
                selectedDob = LocalDate.parse(existing);
            } catch (Exception ignored) {
                selectedDob = null;
            }
        }

        updateDobUi();

        binding.btnOnboardingPickDob.setOnClickListener(v -> showDatePicker());
        binding.btnOnboardingContinue.setOnClickListener(v -> {
            if (selectedDob == null) {
                showToast(getString(R.string.onboarding_dob_required));
                return;
            }

            int age = Period.between(selectedDob, LocalDate.now()).getYears();
            boolean allowMature = age >= 18;

            settings.setBirthDateIso(selectedDob.toString());
            settings.setAllowMatureContent(allowMature);
            settings.setOnboardingStep(2);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_onboardingDobFragment_to_onboardingInterestsFragment);
        });
    }

    private void showDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate initial = selectedDob != null ? selectedDob : LocalDate.of(2000, 1, 1);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    selectedDob = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDobUi();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateDobUi() {
        boolean hasDob = selectedDob != null;
        binding.btnOnboardingContinue.setEnabled(hasDob);

        if (!hasDob) {
            binding.tvOnboardingDobValue.setText(getString(R.string.onboarding_dob_not_selected));
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
        binding.tvOnboardingDobValue.setText(selectedDob.format(formatter));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
