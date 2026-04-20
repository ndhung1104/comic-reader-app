package com.group09.ComicReader.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.SearchResultAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentSearchBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.SearchViewModel;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.RecyclerView;
import com.group09.ComicReader.ui.search.PageNumberAdapter;

public class SearchFragment extends BaseFragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private SearchResultAdapter adapter;
    private boolean gridMode = true;
    private String initialFilter;
    private PageNumberAdapter pageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialFilter = getArguments() == null ? null : getArguments().getString("initialFilter");
        adapter = new SearchResultAdapter(this::openComicDetail);
        binding.rcvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvSearchResults.setAdapter(adapter);

        binding.imgSearchGrid.setOnClickListener(v -> setGridMode(true));
        binding.imgSearchList.setOnClickListener(v -> setGridMode(false));
        setGridMode(true);
        binding.tilSearchQuery.setEndIconOnClickListener(v -> binding.edtSearchQuery.setText(""));

        binding.edtSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.updateQuery(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.cgSearchFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            com.google.android.material.chip.Chip chip = group.findViewById(checked);
            if (chip != null) {
                viewModel.updateFilter(chip.getText().toString());
            }
        });

        viewModel.getFilters().observe(getViewLifecycleOwner(), filters -> {
            if (filters == null || filters.isEmpty())
                return;
            binding.cgSearchFilters.removeAllViews();
            for (String filter : filters) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext(),
                        null, com.google.android.material.R.attr.chipStyle);
                chip.setText(filter);
                chip.setCheckable(true);
                binding.cgSearchFilters.addView(chip);
            }
            selectFilterChip(initialFilter);
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), comics -> {
            adapter.submitList(comics);
            binding.tvSearchResultCount.setText(getString(com.group09.ComicReader.R.string.search_results,
                    comics == null ? 0 : comics.size()));
        });

        binding.btnSearchPrev.setOnClickListener(v -> viewModel.prevPage());
        binding.btnSearchNext.setOnClickListener(v -> viewModel.nextPage());

        // setup horizontal page list
        pageAdapter = new PageNumberAdapter(requireContext(), page -> viewModel.goToPage(page));
        binding.rcvSearchPages
                .setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rcvSearchPages.setAdapter(pageAdapter);

        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), pageNum -> {
            Integer tot = viewModel.getTotalPages().getValue();
            updatePageIndicator(pageNum, tot);
        });

        viewModel.getTotalPages().observe(getViewLifecycleOwner(), tot -> {
            Integer pageNum = viewModel.getCurrentPage().getValue();
            // build pages list
            int t = tot == null || tot <= 0 ? 1 : tot;
            List<Integer> pages = new ArrayList<>();
            for (int i = 0; i < t; i++)
                pages.add(i);
            pageAdapter.setPages(pages);
            updatePageIndicator(pageNum, tot);
        });

        viewModel.loadInitial();
    }

    private void updatePageIndicator(@Nullable Integer pageNum, @Nullable Integer total) {
        int p = pageNum == null ? 0 : pageNum;
        int t = total == null || total <= 0 ? 1 : total;
        // update selected state in page list
        if (pageAdapter != null) {
            pageAdapter.setSelected(p);
            binding.rcvSearchPages.smoothScrollToPosition(p);
        }
        binding.btnSearchPrev.setEnabled(p > 0);
        binding.btnSearchNext.setEnabled(p + 1 < t);
    }

    private void setGridMode(boolean grid) {
        gridMode = grid;
        if (gridMode) {
            binding.rcvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            binding.imgSearchGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
            binding.imgSearchList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        } else {
            binding.rcvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.imgSearchGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
            binding.imgSearchList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
        }
        binding.rcvSearchResults.setAdapter(adapter);
    }

    private void selectFilterChip(@Nullable String filter) {
        String target = filter == null || filter.trim().isEmpty() ? "All" : filter.trim();
        int childCount = binding.cgSearchFilters.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = binding.cgSearchFilters.getChildAt(i);
            if (!(child instanceof com.google.android.material.chip.Chip)) {
                continue;
            }
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) child;
            if (target.equalsIgnoreCase(String.valueOf(chip.getText()))) {
                chip.setChecked(true);
                viewModel.updateFilter(String.valueOf(chip.getText()));
                initialFilter = null;
                return;
            }
        }
        if (childCount > 0) {
            com.google.android.material.chip.Chip first = (com.google.android.material.chip.Chip) binding.cgSearchFilters
                    .getChildAt(0);
            first.setChecked(true);
            viewModel.updateFilter(String.valueOf(first.getText()));
        }
        initialFilter = null;
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        SearchFragmentDirections.ActionSearchToComicDetail action = SearchFragmentDirections
                .actionSearchToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
