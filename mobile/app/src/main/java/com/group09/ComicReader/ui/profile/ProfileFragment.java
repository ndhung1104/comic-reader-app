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
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentProfileBinding;
import com.group09.ComicReader.model.ProfileMenuItem;
import com.group09.ComicReader.viewmodel.ProfileViewModel;

public class ProfileFragment extends BaseFragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private ProfileMenuAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
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

        binding.btnProfileLogout.setOnClickListener(v -> {
            new SessionManager(requireContext()).clear();
            androidx.navigation.NavController navController = Navigation.findNavController(v);
            NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.onboardingFragment, true)
                    .build();
            navController.navigate(R.id.loginFragment, null, navOptions);
        });

        viewModel.loadData();
    }

    private void onMenuClicked(ProfileMenuItem item) {
        if (item.isNavigatesToWallet() && getView() != null) {
            NavDirections action = ProfileFragmentDirections.actionProfileToWallet();
            Navigation.findNavController(getView()).navigate(action);
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