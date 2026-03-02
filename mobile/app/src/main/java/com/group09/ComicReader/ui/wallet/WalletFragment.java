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
import com.group09.ComicReader.databinding.FragmentWalletBinding;
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
        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        packageAdapter = new WalletPackageAdapter(walletPackage -> showToast("Package selected: " + walletPackage.getCoins()));
        transactionAdapter = new WalletTransactionAdapter();

        binding.rcvWalletPackages.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvWalletPackages.setAdapter(packageAdapter);

        binding.rcvWalletTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvWalletTransactions.setAdapter(transactionAdapter);

        binding.btnWalletBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> binding.tvWalletBalanceValue.setText(String.valueOf(balance)));
        viewModel.getPackages().observe(getViewLifecycleOwner(), packageAdapter::submitList);
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactionAdapter::submitList);

        viewModel.loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
