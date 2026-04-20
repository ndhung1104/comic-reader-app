package com.group09.ComicReader.ui.creator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.CreatorRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentCreateComicBinding;
import com.group09.ComicReader.model.ComicResponse;

import java.util.HashMap;
import java.util.Map;

public class CreateComicFragment extends BaseFragment {

    private FragmentCreateComicBinding binding;
    private CreatorRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateComicBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        repository = new CreatorRepository(new ApiClient(requireContext()));

        binding.btnCreate.setOnClickListener(v -> handleCreate());
    }

    private void handleCreate() {
        String title = binding.etTitle.getText().toString().trim();
        String author = binding.etAuthor.getText().toString().trim();
        String genres = binding.etGenres.getText().toString().trim();
        String synopsis = binding.etSynopsis.getText().toString().trim();
        String coverUrl = binding.etCoverUrl.getText().toString().trim();

        if (title.isEmpty()) {
            showToast("Title is required");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("author", author.isEmpty() ? "Unknown" : author);
        body.put("genres", genres);
        body.put("synopsis", synopsis);
        body.put("coverUrl", coverUrl);
        body.put("status", "ONGOING");

        setLoading(true);
        repository.createComic(body, new CreatorRepository.CreateComicCallback() {
            @Override
            public void onSuccess(@NonNull ComicResponse response) {
                if (!isAdded()) return;
                setLoading(false);
                showToast("Comic created: " + response.getTitle());
                Navigation.findNavController(binding.getRoot()).navigateUp();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.btnCreate.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
