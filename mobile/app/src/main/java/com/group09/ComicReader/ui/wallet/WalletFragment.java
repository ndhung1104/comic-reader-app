package com.group09.ComicReader.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.adapter.WalletPackageAdapter;
import com.group09.ComicReader.adapter.WalletTransactionAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.WalletRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentWalletBinding;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.viewmodel.WalletViewModel;

public class WalletFragment extends BaseFragment {

    private FragmentWalletBinding binding;
    private WalletViewModel viewModel;
    private WalletPackageAdapter packageAdapter;
    private WalletTransactionAdapter transactionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        WalletRepository walletRepository = new WalletRepository(apiClient);
        viewModel = new ViewModelProvider(this, new WalletViewModel.Factory(walletRepository))
                .get(WalletViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        packageAdapter = new WalletPackageAdapter(this::handleTopUp);
        transactionAdapter = new WalletTransactionAdapter();

        binding.rcvWalletPackages.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvWalletPackages.setAdapter(packageAdapter);

        binding.rcvWalletTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvWalletTransactions.setAdapter(transactionAdapter);

        binding.btnWalletBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> binding.tvWalletBalanceValue.setText(String.valueOf(balance)));
        viewModel.getPackages().observe(getViewLifecycleOwner(), packageAdapter::submitList);
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactionAdapter::submitList);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getToppingUp().observe(getViewLifecycleOwner(), this::updateLoadingState);

        viewModel.loadData();
    }

    private void handleTopUp(@NonNull WalletPackage walletPackage) {
        viewModel.topUp(walletPackage);
    }

    private void updateLoadingState(@Nullable Boolean ignored) {
        Boolean isLoading = viewModel.getLoading().getValue();
        Boolean isToppingUp = viewModel.getToppingUp().getValue();
        boolean disabled = Boolean.TRUE.equals(isLoading) || Boolean.TRUE.equals(isToppingUp);
        binding.btnWalletBack.setEnabled(!disabled);
        binding.rcvWalletPackages.setEnabled(!disabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
