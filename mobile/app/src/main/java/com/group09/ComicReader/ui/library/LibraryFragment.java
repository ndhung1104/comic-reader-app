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

import com.bumptech.glide.Glide;
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
        binding.imgLibrarySearch.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.searchFragment));
        binding.btnLibraryContinue.setOnClickListener(v -> openFirstRecentIfPossible());

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
            showEmptyState(R.string.library_login_required, R.string.library_empty_subtitle);
            binding.cardLibraryRecentHighlight.setVisibility(View.GONE);
            adapter.submitList(new ArrayList<>());
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
                    showEmptyState(R.string.library_login_required, R.string.library_empty_subtitle);
                    binding.cardLibraryRecentHighlight.setVisibility(View.GONE);
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
        boolean isRecentTab = position == 0;
        binding.cardLibraryRecentHighlight.setVisibility(isRecentTab && !recentItems.isEmpty() ? View.VISIBLE : View.GONE);
        if (isRecentTab && !recentItems.isEmpty()) {
            bindHighlight(recentItems.get(0));
        }
        if (filtered.isEmpty()) {
            showEmptyState(position == 1 ? R.string.library_empty_followed : R.string.library_empty_recent,
                    R.string.library_empty_subtitle);
        } else {
            hideEmptyState();
        }
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

    private void openFirstRecentIfPossible() {
        if (recentItems == null || recentItems.isEmpty()) {
            return;
        }
        openComicDetail(recentItems.get(0));
    }

    private void bindHighlight(@NonNull LibraryItem item) {
        binding.tvLibraryHighlightTitle.setText(item.getTitle());
        String progress = item.getProgressLabel() == null || item.getProgressLabel().trim().isEmpty()
                ? getString(R.string.library_recent_history_subtitle)
                : item.getProgressLabel();
        binding.tvLibraryHighlightBadge.setText(progress);
        binding.tvLibraryHighlightSubtitle.setText(getString(R.string.library_recent_history_subtitle));

        int progressValue = 0;
        if (item.getProgressChapter() != null && item.getTotalChapters() > 0) {
            progressValue = Math.max(0, Math.min(100, (item.getProgressChapter() * 100) / item.getTotalChapters()));
        }
        binding.pbLibraryHighlightProgress.setProgress(progressValue);
        Glide.with(binding.imgLibraryHighlightCover)
                .load(item.getCoverUrl())
                .into(binding.imgLibraryHighlightCover);
    }

    private void showEmptyState(int titleRes, int subtitleRes) {
        binding.llLibraryEmptyState.setVisibility(View.VISIBLE);
        binding.tvLibraryEmpty.setText(titleRes);
        binding.tvLibraryEmptySubtitle.setText(subtitleRes);
    }

    private void hideEmptyState() {
        binding.llLibraryEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
