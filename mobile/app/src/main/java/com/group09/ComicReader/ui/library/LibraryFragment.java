package com.group09.ComicReader.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.LibraryComicAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentLibraryBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.LibraryViewModel;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends BaseFragment {

    private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private LibraryComicAdapter adapter;
    private List<Comic> sourceComics = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new LibraryComicAdapter(this::openComicDetail);
        binding.rcvLibraryComics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvLibraryComics.setAdapter(adapter);

        binding.tabLibraryFilter.addTab(binding.tabLibraryFilter.newTab().setText(R.string.library_tab_recent));
        binding.tabLibraryFilter.addTab(binding.tabLibraryFilter.newTab().setText(R.string.library_tab_favorites));
        binding.tabLibraryFilter.addTab(binding.tabLibraryFilter.newTab().setText(R.string.library_tab_downloads));
        binding.tabLibraryFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewModel.getLibraryComics().observe(getViewLifecycleOwner(), comics -> {
            sourceComics = comics == null ? new ArrayList<>() : comics;
            applyFilter(binding.tabLibraryFilter.getSelectedTabPosition());
        });

        viewModel.loadData();
    }

    private void applyFilter(int position) {
        List<Comic> filtered = new ArrayList<>();
        if (position == 1) {
            for (Comic comic : sourceComics) {
                if (comic.getRating() >= 4.7) {
                    filtered.add(comic);
                }
            }
        } else if (position == 2) {
            for (Comic comic : sourceComics) {
                if (comic.getProgress() != null && comic.getProgress() > 40) {
                    filtered.add(comic);
                }
            }
        } else {
            filtered.addAll(sourceComics);
        }
        adapter.submitList(filtered);
        binding.tvLibraryEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        LibraryFragmentDirections.ActionLibraryToComicDetail action =
                LibraryFragmentDirections.actionLibraryToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
