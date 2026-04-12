package com.group09.ComicReader.ui.browse;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.GenreCardAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentBrowseBinding;
import com.group09.ComicReader.model.Genre;
import com.group09.ComicReader.viewmodel.BrowseViewModel;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends BaseFragment {

    private static final String FILTER_ALL = "All";
    private static final String FILTER_COMPLETED = "Completed";
    private static final String FILTER_ONGOING = "Ongoing";
    private static final String VIEW_ALL_NAME = "View All";

    private FragmentBrowseBinding binding;
    private BrowseViewModel viewModel;
    private GenreCardAdapter genreAdapter;
    private GridLayoutManager layoutManager;
    private final List<Genre> displayedGenres = new ArrayList<>();
    private String selectedFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(BrowseViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupAdapter();
        setupFilterTabs();
        setupSearchButton();
        observeData();
        updateFilterButtons();
        viewModel.loadGenres();
    }

    private void setupAdapter() {
        int spanCount = resolveSpanCount();
        genreAdapter = new GenreCardAdapter(this::openGenreDetail);
        genreAdapter.setSpanCount(spanCount);

        layoutManager = new GridLayoutManager(requireContext(), spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < 0 || position >= displayedGenres.size()) {
                    return 1;
                }

                Genre genre = displayedGenres.get(position);
                if (genre.isLarge() || genre.isMedium()) {
                    return spanCount > 2 ? 2 : spanCount;
                }
                return 1;
            }
        });

        binding.rcvBrowseGenres.setLayoutManager(layoutManager);
        binding.rcvBrowseGenres.setAdapter(genreAdapter);
    }

    private int resolveSpanCount() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        return screenWidthDp >= 840 ? 4 : 2;
    }

    private void setupFilterTabs() {
        binding.btnBrowseFilterAll.setOnClickListener(v -> applyFilter(FILTER_ALL));
        binding.btnBrowseFilterCompleted.setOnClickListener(v -> applyFilter(FILTER_COMPLETED));
        binding.btnBrowseFilterOngoing.setOnClickListener(v -> applyFilter(FILTER_ONGOING));
        binding.btnBrowseSort.setOnClickListener(v -> {
            if (FILTER_ALL.equals(selectedFilter)) {
                applyFilter(FILTER_ONGOING);
            } else if (FILTER_ONGOING.equals(selectedFilter)) {
                applyFilter(FILTER_COMPLETED);
            } else {
                applyFilter(FILTER_ALL);
            }
        });
    }

    private void observeData() {
        viewModel.getGenres().observe(getViewLifecycleOwner(), genres -> {
            displayedGenres.clear();
            if (genres != null) {
                displayedGenres.addAll(genres);
            }
            genreAdapter.submitList(displayedGenres);
            if (binding != null) {
                binding.rcvBrowseGenres.requestLayout();
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });
    }

    private void updateFilterButtonState(@NonNull MaterialButton button, boolean isSelected) {
        int backgroundColor = ContextCompat.getColor(
                requireContext(), isSelected ? R.color.primary : android.R.color.transparent);
        int textColor = ContextCompat.getColor(
                requireContext(), isSelected ? R.color.on_primary : R.color.on_surface_variant);

        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
    }

    private void updateFilterButtons() {
        updateFilterButtonState(binding.btnBrowseFilterAll, FILTER_ALL.equals(selectedFilter));
        updateFilterButtonState(binding.btnBrowseFilterCompleted, FILTER_COMPLETED.equals(selectedFilter));
        updateFilterButtonState(binding.btnBrowseFilterOngoing, FILTER_ONGOING.equals(selectedFilter));
    }

    private void applyFilter(@NonNull String filter) {
        selectedFilter = filter;
        updateFilterButtons();
        viewModel.applyFilter(filter);
    }

    private void setupSearchButton() {
        binding.btnBrowseSearch.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.searchFragment));
    }

    private void openGenreDetail(Genre genre) {
        if (genre == null || getView() == null) {
            return;
        }
        android.os.Bundle args = new android.os.Bundle();
        if (VIEW_ALL_NAME.equals(genre.getName())) {
            args.putString("initialFilter", null);
        } else {
            args.putString("initialFilter", genre.getCategoryId());
        }
        Navigation.findNavController(getView()).navigate(R.id.searchFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
