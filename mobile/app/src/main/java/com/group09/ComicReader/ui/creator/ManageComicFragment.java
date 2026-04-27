package com.group09.ComicReader.ui.creator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.CreatorRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentManageComicBinding;
import com.group09.ComicReader.model.AiSummaryResponse;
import com.group09.ComicReader.model.ComicResponse;

public class ManageComicFragment extends BaseFragment {

    private FragmentManageComicBinding binding;
    private CreatorRepository repository;
    private ComicResponse comic;
    private AiSummaryResponse currentSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageComicBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            comic = (ComicResponse) getArguments().getSerializable("comic");
        }

        if (comic == null) {
            showToast("Error: Comic data not found");
            Navigation.findNavController(view).navigateUp();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.tvComicTitle.setText(comic.getTitle());

        repository = new CreatorRepository(new ApiClient(requireContext()));

        setupListeners();
        loadLatestSummary();
    }

    private void setupListeners() {
        binding.btnGenerate.setOnClickListener(v -> handleGenerate());
        binding.btnRegenerate.setOnClickListener(v -> handleGenerate());
        binding.btnApprove.setOnClickListener(v -> handleModerate("APPROVED"));
    }

    private void loadLatestSummary() {
        setLoading(true);
        repository.getSummaryHistory(comic.getId(), null, new CreatorRepository.SummaryListCallback() {
            @Override
            public void onSuccess(@NonNull java.util.List<AiSummaryResponse> responses) {
                if (!isAdded()) return;
                setLoading(false);
                if (!responses.isEmpty()) {
                    displaySummary(responses.get(0));
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                setLoading(false);
                // History might be empty, that's fine
            }
        });
    }

    private void handleGenerate() {
        setLoading(true);
        repository.generateSummary(comic.getId(), null, new CreatorRepository.SummaryCallback() {
            @Override
            public void onSuccess(@NonNull AiSummaryResponse response) {
                if (!isAdded()) return;
                setLoading(false);
                displaySummary(response);
                showToast("AI Summary generated successfully");
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void handleModerate(String status) {
        if (currentSummary == null) return;

        setLoading(true);
        repository.moderateSummary(currentSummary.getId(), status, null, new CreatorRepository.SummaryCallback() {
            @Override
            public void onSuccess(@NonNull AiSummaryResponse response) {
                if (!isAdded()) return;
                setLoading(false);
                displaySummary(response);
                showToast("Summary " + status.toLowerCase());
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void displaySummary(AiSummaryResponse summary) {
        currentSummary = summary;
        binding.tvSummaryContent.setText(summary.getContent());
        binding.tvSummaryStatus.setText("Status: " + summary.getStatus());
        
        if ("REVIEW".equals(summary.getStatus())) {
            binding.btnGenerate.setVisibility(View.GONE);
            binding.btnApprove.setVisibility(View.VISIBLE);
            binding.btnRegenerate.setVisibility(View.VISIBLE);
        } else {
            binding.btnGenerate.setVisibility(View.VISIBLE);
            binding.btnApprove.setVisibility(View.GONE);
            binding.btnRegenerate.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGenerate.setEnabled(!loading);
        binding.btnApprove.setEnabled(!loading);
        binding.btnRegenerate.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
