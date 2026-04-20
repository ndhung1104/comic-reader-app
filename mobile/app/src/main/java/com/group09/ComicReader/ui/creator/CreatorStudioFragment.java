package com.group09.ComicReader.ui.creator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.CreatorRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.model.ComicResponse;
import com.group09.ComicReader.model.ImportJobResponse;
import com.group09.ComicReader.databinding.FragmentCreatorStudioBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class CreatorStudioFragment extends BaseFragment {

    private FragmentCreatorStudioBinding binding;
    private CreatorRepository repository;
    private SessionManager sessionManager;
    private CreatorComicsAdapter comicsAdapter;
    private ImportJobsAdapter jobsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatorStudioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        sessionManager = new SessionManager(requireContext());
        repository = new CreatorRepository(new ApiClient(requireContext()));

        setupAdapters();
        setupListeners();
        loadData();
    }

    private void setupAdapters() {
        comicsAdapter = new CreatorComicsAdapter(new CreatorComicsAdapter.OnComicActionListener() {
            @Override
            public void onComicClick(ComicResponse comic) {
                // Navigate to manage/edit current comic if we had a fragment for it
                showToast("Managing " + comic.getTitle());
            }

            @Override
            public void onComicDelete(ComicResponse comic) {
                confirmDelete(comic);
            }
        });

        jobsAdapter = new ImportJobsAdapter();

        binding.rvComics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvComics.setAdapter(comicsAdapter);

        binding.rvJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvJobs.setAdapter(jobsAdapter);
    }

    private void setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        binding.btnEnqueue.setOnClickListener(v -> handleEnqueue());
        binding.btnCreateNew.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.action_creatorStudioFragment_to_createComicFragment);
        });
    }

    private void loadData() {
        if (!sessionManager.hasToken()) return;
        
        binding.swipeRefresh.setRefreshing(true);
        
        // Load Comics
        repository.getMyComics(0, 50, new CreatorRepository.ListComicsCallback() {
            @Override
            public void onSuccess(@NonNull List<ComicResponse> comics, int page, int totalPages) {
                if (!isAdded()) return;
                comicsAdapter.submitList(comics);
                binding.tvStatComics.setText(String.valueOf(comics.size()));
                checkDataLoaded();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                showToast(message);
                checkDataLoaded();
            }
        });

        // Load Jobs
        repository.getMyJobs(0, 50, new CreatorRepository.ListJobsCallback() {
            @Override
            public void onSuccess(@NonNull List<ImportJobResponse> jobs, int page, int totalPages) {
                if (!isAdded()) return;
                jobsAdapter.submitList(jobs);
                long activeCount = jobs.stream()
                        .filter(j -> "RUNNING".equals(j.getStatus()) || "QUEUED".equals(j.getStatus()))
                        .count();
                binding.tvStatJobs.setText(String.valueOf(activeCount));
                checkDataLoaded();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                showToast(message);
                checkDataLoaded();
            }
        });
    }

    private int loadingTrack = 0;
    private void checkDataLoaded() {
        loadingTrack++;
        if (loadingTrack >= 2) {
            loadingTrack = 0;
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private void handleEnqueue() {
        String url = binding.etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            showToast("Please enter an URL or slug");
            return;
        }

        binding.swipeRefresh.setRefreshing(true);
        repository.enqueueImport(url, "OTRUYEN", new CreatorRepository.EnqueueCallback() {
            @Override
            public void onSuccess(@NonNull ImportJobResponse response) {
                if (!isAdded()) return;
                binding.etUrl.setText("");
                showToast("Import task enqueued");
                loadData();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                showToast(message);
            }
        });
    }

    private void confirmDelete(ComicResponse comic) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Comic")
                .setMessage("Are you sure you want to delete '" + comic.getTitle() + "'? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteComic(comic);
                })
                .show();
    }

    private void deleteComic(ComicResponse comic) {
        binding.swipeRefresh.setRefreshing(true);
        repository.deleteComic(comic.getId(), new CreatorRepository.DeleteComicCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                showToast("Comic deleted successfully");
                loadData();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
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
