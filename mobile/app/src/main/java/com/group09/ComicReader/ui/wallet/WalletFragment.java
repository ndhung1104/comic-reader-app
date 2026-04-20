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

import com.group09.ComicReader.BuildConfig;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.WalletPackageAdapter;
import com.group09.ComicReader.adapter.WalletTransactionAdapter;
import com.group09.ComicReader.ads.RewardedAdController;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.WalletRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentWalletBinding;
import com.group09.ComicReader.model.VipStatusResponse;
import com.group09.ComicReader.model.WalletPackage;
import com.group09.ComicReader.viewmodel.WalletViewModel;

public class WalletFragment extends BaseFragment {

    private FragmentWalletBinding binding;
    private WalletViewModel viewModel;
    private WalletPackageAdapter packageAdapter;
    private WalletTransactionAdapter transactionAdapter;
    private RewardedAdController rewardedAdController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        WalletRepository walletRepository = new WalletRepository(apiClient);
        viewModel = new ViewModelProvider(this, new WalletViewModel.Factory(walletRepository))
                .get(WalletViewModel.class);
        rewardedAdController = new RewardedAdController(requireContext(), BuildConfig.ADMOB_REWARDED_UNIT_ID);
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
        binding.btnWalletRewardCoins.setOnClickListener(v -> showRewardedAd(WalletRepository.REWARD_TYPE_COIN));
        binding.btnWalletRewardPoints.setOnClickListener(v -> showRewardedAd(WalletRepository.REWARD_TYPE_POINT));
        binding.btnWalletVipMonthly.setOnClickListener(v -> viewModel.purchaseVip("MONTHLY"));
        binding.btnWalletVipYearly.setOnClickListener(v -> viewModel.purchaseVip("YEARLY"));

        rewardedAdController.preload();

        viewModel.getCoinBalance().observe(getViewLifecycleOwner(), balance -> binding.tvWalletBalanceValue.setText(String.valueOf(balance)));
        viewModel.getPointBalance().observe(getViewLifecycleOwner(), balance -> binding.tvWalletPointValue.setText(String.valueOf(balance)));
        viewModel.getPackages().observe(getViewLifecycleOwner(), packageAdapter::submitList);
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactionAdapter::submitList);
        viewModel.getVipStatus().observe(getViewLifecycleOwner(), this::renderVipStatus);
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

    private void showRewardedAd(@NonNull String rewardType) {
        if (getActivity() == null) {
            return;
        }
        rewardedAdController.show(requireActivity(), new RewardedAdController.Listener() {
            private boolean rewardClaimed;

            @Override
            public void onRewardEarned(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                if (rewardClaimed) {
                    return;
                }
                rewardClaimed = true;
                viewModel.claimAdReward(rewardType);
            }

            @Override
            public void onAdUnavailable(@NonNull String message) {
                showToast(message);
            }

            @Override
            public void onAdClosed() {
            }
        });
    }

    private void renderVipStatus(@Nullable VipStatusResponse vipStatusResponse) {
        if (binding == null || vipStatusResponse == null) {
            return;
        }

        boolean active = vipStatusResponse.isVip();
        String statusText = active
                ? getString(R.string.wallet_vip_active_format, normalizePlan(vipStatusResponse.getPlan()), safeDate(vipStatusResponse.getEndDate()))
                : getString(R.string.wallet_vip_inactive);
        binding.tvWalletVipStatus.setText(statusText);
        binding.btnWalletVipMonthly.setEnabled(!active);
        binding.btnWalletVipYearly.setEnabled(!active);
    }

    @NonNull
    private String normalizePlan(@Nullable String plan) {
        if (plan == null || plan.trim().isEmpty()) {
            return getString(R.string.wallet_vip_plan_unknown);
        }
        if ("YEARLY".equalsIgnoreCase(plan)) {
            return getString(R.string.wallet_vip_plan_yearly);
        }
        if ("MONTHLY".equalsIgnoreCase(plan)) {
            return getString(R.string.wallet_vip_plan_monthly);
        }
        return plan;
    }

    @NonNull
    private String safeDate(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.wallet_vip_plan_unknown);
        }
        int separatorIndex = value.indexOf('T');
        return separatorIndex > 0 ? value.substring(0, separatorIndex) : value;
    }

    private void updateLoadingState(@Nullable Boolean ignored) {
        Boolean isLoading = viewModel.getLoading().getValue();
        Boolean isToppingUp = viewModel.getToppingUp().getValue();
        boolean disabled = Boolean.TRUE.equals(isLoading) || Boolean.TRUE.equals(isToppingUp);
        binding.btnWalletBack.setEnabled(!disabled);
        binding.rcvWalletPackages.setAlpha(disabled ? 0.6f : 1f);
        binding.btnWalletRewardCoins.setEnabled(!disabled);
        binding.btnWalletRewardPoints.setEnabled(!disabled);
        binding.btnWalletVipMonthly.setEnabled(!disabled && (viewModel.getVipStatus().getValue() == null || !viewModel.getVipStatus().getValue().isVip()));
        binding.btnWalletVipYearly.setEnabled(!disabled && (viewModel.getVipStatus().getValue() == null || !viewModel.getVipStatus().getValue().isVip()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
