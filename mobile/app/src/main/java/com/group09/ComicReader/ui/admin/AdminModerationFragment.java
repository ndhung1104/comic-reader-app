package com.group09.ComicReader.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminModerationBinding;
import com.group09.ComicReader.model.AiSummaryResponse;
import com.group09.ComicReader.model.ImportJobResponse;
import com.group09.ComicReader.data.CreatorRepository;

import java.util.List;

public class AdminModerationFragment extends BaseFragment {

    private FragmentAdminModerationBinding binding;
    private AdminRepository adminRepository;
    private CreatorRepository creatorRepository;
    private ModerationImportsAdapter importsAdapter;
    private ModerationSummariesAdapter summariesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminModerationBinding.inflate(inflater, container, false);
        adminRepository = new AdminRepository(new ApiClient(requireContext()));
        creatorRepository = new CreatorRepository(new ApiClient(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupTabs();
        setupRefresh();

        binding.toolbar.setNavigationOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigateUp());

        loadData();
    }

    private void setupRecyclerView() {
        binding.rcvModeration.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        importsAdapter = new ModerationImportsAdapter(new ModerationImportsAdapter.OnModerationListener() {
            @Override
            public void onApprove(ImportJobResponse job) {
                moderateImport(job.getId(), "APPROVED");
            }

            @Override
            public void onReject(ImportJobResponse job) {
                moderateImport(job.getId(), "REJECTED");
            }
        });

        summariesAdapter = new ModerationSummariesAdapter(new ModerationSummariesAdapter.OnModerationListener() {
            @Override
            public void onApprove(AiSummaryResponse summary) {
                moderateSummary(summary.getId(), "APPROVED");
            }

            @Override
            public void onReject(AiSummaryResponse summary) {
                moderateSummary(summary.getId(), "REJECTED");
            }
        });

        // Default to imports
        binding.rcvModeration.setAdapter(importsAdapter);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.rcvModeration.setAdapter(importsAdapter);
                    loadImports();
                } else {
                    binding.rcvModeration.setAdapter(summariesAdapter);
                    loadSummaries();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        if (binding.tabLayout.getSelectedTabPosition() == 0) {
            loadImports();
        } else {
            loadSummaries();
        }
    }

    private void loadImports() {
        binding.swipeRefresh.setRefreshing(true);
        adminRepository.getPendingImports(new AdminRepository.ImportListCallback() {
            @Override
            public void onSuccess(@NonNull List<ImportJobResponse> list) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                importsAdapter.submitList(list);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                showToast(message);
            }
        });
    }

    private void loadSummaries() {
        binding.swipeRefresh.setRefreshing(true);
        adminRepository.getPendingSummaries(new AdminRepository.SummaryListCallback() {
            @Override
            public void onSuccess(@NonNull List<AiSummaryResponse> list) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                summariesAdapter.submitList(list);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                showToast(message);
            }
        });
    }

    private void moderateImport(long id, String status) {
        adminRepository.moderateImport(id, status, null, new AdminRepository.ImportActionCallback() {
            @Override
            public void onSuccess(@NonNull ImportJobResponse job) {
                if (!isAdded()) return;
                showToast("Import " + status.toLowerCase());
                loadImports();
            }

            @Override
            public void onError(@NonNull String message) {
                showToast(message);
            }
        });
    }

    private void moderateSummary(long id, String status) {
        creatorRepository.moderateSummary(id, status, null, new CreatorRepository.SummaryCallback() {
            @Override
            public void onSuccess(@NonNull AiSummaryResponse response) {
                if (!isAdded()) return;
                showToast("Summary " + status.toLowerCase());
                loadSummaries();
            }

            @Override
            public void onError(@NonNull String message) {
                showToast(message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
