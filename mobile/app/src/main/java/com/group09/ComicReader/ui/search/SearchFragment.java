package com.group09.ComicReader.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.SearchResultAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentSearchBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.SearchViewModel;

public class SearchFragment extends BaseFragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private SearchResultAdapter adapter;

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
        adapter = new SearchResultAdapter(this::openComicDetail);
        binding.rcvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvSearchResults.setAdapter(adapter);

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
            String filter = "All";
            if (checked == R.id.chip_search_action) {
                filter = "Action";
            } else if (checked == R.id.chip_search_romance) {
                filter = "Romance";
            } else if (checked == R.id.chip_search_fantasy) {
                filter = "Fantasy";
            }
            viewModel.updateFilter(filter);
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), comics -> {
            adapter.submitList(comics);
            binding.tvSearchResultCount.setText(getString(com.group09.ComicReader.R.string.search_results,
                    comics == null ? 0 : comics.size()));
        });

        viewModel.loadInitial();
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        SearchFragmentDirections.ActionSearchToComicDetail action =
                SearchFragmentDirections.actionSearchToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
