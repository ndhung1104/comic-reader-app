package com.group09.ComicReader.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.LibraryComicAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.LibraryRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentLibraryBinding;
import com.group09.ComicReader.model.LibraryItem;
import com.group09.ComicReader.ui.reader.ReaderActivity;
import com.group09.ComicReader.viewmodel.LibraryViewModel;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends BaseFragment {

    private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private LibraryComicAdapter adapter;
    private List<LibraryItem> followedItems = new ArrayList<>();
    private List<LibraryItem> recentItems = new ArrayList<>();
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        LibraryRepository libraryRepository = new LibraryRepository(apiClient);
        viewModel = new ViewModelProvider(this, new LibraryViewModel.Factory(libraryRepository))
                .get(LibraryViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        adapter = new LibraryComicAdapter(this::openComicDetail);
        binding.rcvLibraryComics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvLibraryComics.setAdapter(adapter);

        binding.tabLibraryFilter.addTab(binding.tabLibraryFilter.newTab().setText(R.string.library_tab_recent));
        binding.tabLibraryFilter.addTab(binding.tabLibraryFilter.newTab().setText(R.string.library_tab_followed));
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

        if (!sessionManager.hasToken()) {
            binding.tvLibraryEmpty.setText(R.string.library_login_required);
            binding.tvLibraryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        viewModel.getFollowedComics().observe(getViewLifecycleOwner(), items -> {
            followedItems = items == null ? new ArrayList<>() : items;
            applyFilter(binding.tabLibraryFilter.getSelectedTabPosition());
        });
        viewModel.getRecentReads().observe(getViewLifecycleOwner(), items -> {
            recentItems = items == null ? new ArrayList<>() : items;
            applyFilter(binding.tabLibraryFilter.getSelectedTabPosition());
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                if ("Session expired. Please log in again.".equals(message)) {
                    sessionManager.clear();
                    binding.tvLibraryEmpty.setText(R.string.library_login_required);
                    binding.tvLibraryEmpty.setVisibility(View.VISIBLE);
                    adapter.submitList(new ArrayList<>());
                    return;
                }
                showToast(message);
            }
        });

        viewModel.loadData();
    }

    private void applyFilter(int position) {
        List<LibraryItem> filtered = position == 1 ? followedItems : recentItems;
        adapter.submitList(filtered);
        binding.tvLibraryEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvLibraryEmpty.setText(position == 1
                ? R.string.library_empty_followed
                : R.string.library_empty_recent);
    }

    private void openComicDetail(LibraryItem item) {
        if (item == null) {
            return;
        }
        int tabPosition = binding.tabLibraryFilter.getSelectedTabPosition();
        if (tabPosition == 0 && item.getResumeChapterId() != null && item.getResumeChapterNumber() != null) {
            startActivity(ReaderActivity.createIntent(
                    requireContext(),
                    item.getComicId(),
                    item.getResumeChapterId(),
                    item.getResumeChapterNumber()));
            return;
        }
        if (getView() == null) {
            return;
        }
        LibraryFragmentDirections.ActionLibraryToComicDetail action =
                LibraryFragmentDirections.actionLibraryToComicDetail(item.getComicId());
        Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
