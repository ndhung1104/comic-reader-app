package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ReaderPageAdapter;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.ReaderProgressStore;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.ReaderPage;
import com.group09.ComicReader.viewmodel.ReaderViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_CHAPTER = "extra_chapter";
    private static final long SAVE_PROGRESS_DEBOUNCE_MS = 1500L;

    public static Intent createIntent(@NonNull Context context, int comicId, int chapterId, int chapterNumber) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_COMIC_ID, comicId);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_CHAPTER, chapterNumber);
        return intent;
    }

    private ActivityReaderBinding binding;
    private int comicId;
    private int chapterId;
    private int chapterNumber;
    private ReaderPageAdapter pageAdapter;
    private ReaderViewModel viewModel;
    private LinearLayoutManager layoutManager;
    private ReaderProgressStore readerProgressStore;
    private ReaderProgressStore.ReaderProgress restoredProgress;
    private boolean restoredPositionApplied;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveProgressRunnable = this::saveCurrentReadingProgress;
    private final RecyclerView.OnScrollListener progressScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (dy != 0) {
                scheduleSaveReadingProgress();
            }
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                scheduleSaveReadingProgress();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comicId = getIntent().getIntExtra(EXTRA_COMIC_ID, 1);
        chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 1);
        chapterNumber = getIntent().getIntExtra(EXTRA_CHAPTER, 1);
        readerProgressStore = new ReaderProgressStore(this);
        restoredProgress = readerProgressStore.getProgressForChapter(comicId, chapterId);

        ApiClient apiClient = new ApiClient(this);
        ReaderRepository readerRepository = new ReaderRepository(apiClient);
        ReaderViewModel.Factory factory = new ReaderViewModel.Factory(readerRepository);
        viewModel = new ViewModelProvider(this, factory).get(ReaderViewModel.class);

        pageAdapter = new ReaderPageAdapter();
        layoutManager = new LinearLayoutManager(this);
        binding.rcvReaderPages.setLayoutManager(layoutManager);
        binding.rcvReaderPages.setItemViewCacheSize(6);
        binding.rcvReaderPages.setAdapter(pageAdapter);
        binding.rcvReaderPages.addOnScrollListener(progressScrollListener);

        binding.tvReaderTitle.setText(getString(R.string.app_name));
        ComicRepository.getInstance().getComicById(comicId, new ComicRepository.ComicCallback() {
            @Override
            public void onSuccess(Comic fetchedComic) {
                if (fetchedComic != null && binding != null) {
                    binding.tvReaderTitle.setText(fetchedComic.getTitle());
                }
            }

            @Override
            public void onError(String error) {
            }
        });
        binding.tvReaderChapter.setText(getString(R.string.reader_chapter, chapterNumber));

        binding.btnReaderBack.setOnClickListener(v -> {
            saveCurrentReadingProgress();
            finish();
        });

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            binding.prgReaderLoading.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        viewModel.getPages().observe(this, pages -> {
            List<ReaderPage> safePages = pages == null ? Collections.emptyList() : new ArrayList<>(pages);
            pageAdapter.submitList(safePages, () -> {
                restoreReadingPositionIfNeeded();
                preloadInitialPages(safePages);
            });
            boolean hasPages = !safePages.isEmpty();
            binding.tvReaderEmpty.setVisibility(hasPages ? android.view.View.GONE : android.view.View.VISIBLE);
        });
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                binding.tvReaderEmpty.setVisibility(android.view.View.VISIBLE);
                binding.tvReaderEmpty.setText(message);
            }
        });

        if (chapterId <= 0) {
            binding.tvReaderEmpty.setText(getString(R.string.reader_invalid_chapter));
            binding.tvReaderEmpty.setVisibility(android.view.View.VISIBLE);
            return;
        }
        viewModel.loadChapterPages(chapterId);
    }

    @Override
    protected void onPause() {
        saveCurrentReadingProgress();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(saveProgressRunnable);
        if (binding != null) {
            binding.rcvReaderPages.removeOnScrollListener(progressScrollListener);
        }
        super.onDestroy();
    }

    private void preloadInitialPages(@NonNull List<ReaderPage> pages) {
        int preloadCount = Math.min(3, pages.size());
        for (int index = 0; index < preloadCount; index++) {
            ReaderPage page = pages.get(index);
            if (page.getImageUrl() == null || page.getImageUrl().trim().isEmpty()) {
                continue;
            }
            Glide.with(this)
                    .load(page.getImageUrl())
                    .preload();
        }
    }

    private void restoreReadingPositionIfNeeded() {
        if (restoredPositionApplied || restoredProgress == null || pageAdapter.getItemCount() == 0) {
            return;
        }
        restoredPositionApplied = true;

        int restoredPosition = restoredProgress.getPagePosition();
        if (restoredPosition < 0) {
            return;
        }
        int maxPosition = Math.max(0, pageAdapter.getItemCount() - 1);
        int targetPosition = Math.min(restoredPosition, maxPosition);
        int targetOffset = restoredProgress.getOffset();

        binding.rcvReaderPages.post(() ->
                layoutManager.scrollToPositionWithOffset(targetPosition, targetOffset));
    }

    private void scheduleSaveReadingProgress() {
        progressHandler.removeCallbacks(saveProgressRunnable);
        progressHandler.postDelayed(saveProgressRunnable, SAVE_PROGRESS_DEBOUNCE_MS);
    }

    private void saveCurrentReadingProgress() {
        if (layoutManager == null) {
            return;
        }
        int position = layoutManager.findFirstVisibleItemPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        View firstVisibleView = layoutManager.findViewByPosition(position);
        int offset = firstVisibleView == null ? 0 : firstVisibleView.getTop();
        readerProgressStore.saveProgress(comicId, chapterId, chapterNumber, position, offset);
    }
}
