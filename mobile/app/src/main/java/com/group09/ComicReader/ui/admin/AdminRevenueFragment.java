package com.group09.ComicReader.ui.admin;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.adapter.DailyRevenueAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminRevenueBinding;
import com.group09.ComicReader.viewmodel.AdminRevenueViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminRevenueFragment extends BaseFragment {

    private FragmentAdminRevenueBinding binding;
    private AdminRevenueViewModel viewModel;
    private DailyRevenueAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminRevenueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ApiClient apiClient = new ApiClient(requireContext());
        AdminRepository repository = new AdminRepository(apiClient);
        viewModel = new ViewModelProvider(this, new AdminRevenueViewModel.Factory(repository)).get(AdminRevenueViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(view).navigateUp()
        );

        adapter = new DailyRevenueAdapter();
        binding.rcvDailyRevenue.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvDailyRevenue.setAdapter(adapter);

        setupObservers();
        setupClickListeners();

        // Default to Today
        loadDataForPeriod(0);
    }

    private void setupClickListeners() {
        binding.btnToday.setOnClickListener(v -> loadDataForPeriod(0));
        binding.btn7d.setOnClickListener(v -> loadDataForPeriod(6));
        binding.btn30d.setOnClickListener(v -> loadDataForPeriod(29));
    }

    private void loadDataForPeriod(int daysAgo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(daysAgo);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            viewModel.loadData(from.format(formatter), to.format(formatter));
        } else {
            // Unlikely to execute as Android API levels for java.time are via desugaring plugins generally, but added a fallback.
            viewModel.loadData("2020-01-01", "2099-12-31");
        }
    }

    private void setupObservers() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getTotalTopUp().observe(getViewLifecycleOwner(), total -> 
            binding.tvTotalTopup.setText(String.valueOf(total)));
            
        viewModel.getTotalPurchase().observe(getViewLifecycleOwner(), total -> 
            binding.tvTotalPurchase.setText(String.valueOf(total)));
            
        viewModel.getTotalVip().observe(getViewLifecycleOwner(), total -> 
            binding.tvTotalVip.setText(String.valueOf(total)));
            
        viewModel.getTotalRevenue().observe(getViewLifecycleOwner(), total -> 
            binding.tvTotalRevenue.setText(String.valueOf(total)));

        viewModel.getDailyList().observe(getViewLifecycleOwner(), list -> 
            adapter.submitList(list));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
