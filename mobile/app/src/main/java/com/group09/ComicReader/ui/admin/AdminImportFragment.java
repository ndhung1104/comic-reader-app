package com.group09.ComicReader.ui.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.group09.ComicReader.R;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminImportBinding;
import com.group09.ComicReader.model.ComicResponse;

public class AdminImportFragment extends BaseFragment {

    private FragmentAdminImportBinding binding;
    private AdminRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminImportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ApiClient apiClient = new ApiClient(requireContext());
        repository = new AdminRepository(apiClient);

        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(view).navigateUp()
        );

        binding.btnImport.setOnClickListener(v -> handleImport());
    }

    private void handleImport() {
        String urlOrSlug = binding.etUrl.getText().toString().trim();
        if (urlOrSlug.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an URL or slug", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceType = binding.rgSourceType.getCheckedRadioButtonId() == R.id.rb_otruyen 
                ? "OTRUYEN" : "UNKNOWN";

        setLoadingState(true);
        binding.tvResult.setVisibility(View.GONE);

        repository.importComic(urlOrSlug, sourceType, new AdminRepository.ImportComicCallback() {
            @Override
            public void onSuccess(@NonNull ComicResponse response) {
                if (isAdded()) {
                    setLoadingState(false);
                    binding.etUrl.setText("");
                    binding.tvResult.setText(getString(R.string.admin_import_success) + "\nComic: " + response.getTitle());
                    binding.tvResult.setTextColor(getResources().getColor(R.color.positive_color));
                    binding.tvResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    setLoadingState(false);
                    binding.tvResult.setText("Failed: " + message);
                    binding.tvResult.setTextColor(getResources().getColor(R.color.danger_color));
                    binding.tvResult.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnImport.setEnabled(!isLoading);
        binding.btnImport.setText(isLoading ? R.string.admin_import_importing : R.string.admin_import_button);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
