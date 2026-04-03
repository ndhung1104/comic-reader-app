package com.group09.ComicReader.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.adapter.AdminPackageAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminPackagesBinding;
import com.group09.ComicReader.viewmodel.AdminPackagesViewModel;

public class AdminPackagesFragment extends BaseFragment {

    private FragmentAdminPackagesBinding binding;
    private AdminPackagesViewModel viewModel;
    private AdminPackageAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminPackagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ApiClient apiClient = new ApiClient(requireContext());
        AdminRepository repository = new AdminRepository(apiClient);
        viewModel = new ViewModelProvider(this, new AdminPackagesViewModel.Factory(repository)).get(AdminPackagesViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(view).navigateUp()
        );

        adapter = new AdminPackageAdapter((id, isActive) -> {
            viewModel.togglePackageStatus(id, isActive);
        });
        
        binding.rcvPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvPackages.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> showCreateDialog());

        setupObservers();
        viewModel.loadPackages();
    }

    private void showCreateDialog() {
        android.content.Context ctx = requireContext();
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        int textColor = getResources().getColor(com.group09.ComicReader.R.color.black);
        int hintColor = getResources().getColor(com.group09.ComicReader.R.color.text_muted);

        EditText etName = new EditText(ctx);
        etName.setHint("Package Name (e.g. Starter Pack)");
        etName.setTextColor(textColor);
        etName.setHintTextColor(hintColor);
        layout.addView(etName);

        EditText etCoins = new EditText(ctx);
        etCoins.setHint("Coins (e.g. 500)");
        etCoins.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etCoins.setTextColor(textColor);
        etCoins.setHintTextColor(hintColor);
        layout.addView(etCoins);

        EditText etPrice = new EditText(ctx);
        etPrice.setHint("Price Label (e.g. $4.99)");
        etPrice.setTextColor(textColor);
        etPrice.setHintTextColor(hintColor);
        layout.addView(etPrice);

        EditText etBonus = new EditText(ctx);
        etBonus.setHint("Bonus Label (Optional, e.g. +100 Bonus)");
        etBonus.setTextColor(textColor);
        etBonus.setHintTextColor(hintColor);
        layout.addView(etBonus);

        EditText etSort = new EditText(ctx);
        etSort.setHint("Sort Order (e.g. 1)");
        etSort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etSort.setTextColor(textColor);
        etSort.setHintTextColor(hintColor);
        layout.addView(etSort);

        new AlertDialog.Builder(ctx)
                .setTitle("Create Package")
                .setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String price = etPrice.getText().toString();
                    String bonus = etBonus.getText().toString();
                    int coins = 0;
                    int sort = 0;
                    try { coins = Integer.parseInt(etCoins.getText().toString()); } catch (Exception ignored) {}
                    try { sort = Integer.parseInt(etSort.getText().toString()); } catch (Exception ignored) {}
                    
                    if (!name.isEmpty() && !price.isEmpty() && coins > 0) {
                        viewModel.createPackage(name, coins, price, bonus, sort);
                    } else {
                        Toast.makeText(ctx, "Please fill required fields", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupObservers() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.getPackagesList().observe(getViewLifecycleOwner(), list -> 
            adapter.submitList(list));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
