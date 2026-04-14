package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ReaderCommentsFooterAdapter;
import com.group09.ComicReader.adapter.ReaderPageAdapter;
import com.group09.ComicReader.common.error.ErrorParser;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.AppSettingsStore;
import com.group09.ComicReader.data.local.ReaderProgressStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.ComicChapterResponse;
import com.group09.ComicReader.model.ReaderAudioPage;
import com.group09.ComicReader.model.ReaderPage;
import com.group09.ComicReader.ui.reader.audio.MediaPlayerReaderAudioPlayer;
import com.group09.ComicReader.ui.reader.audio.ReaderAudioController;
import com.group09.ComicReader.ui.reader.audio.ReaderAudioError;
import com.group09.ComicReader.viewmodel.CommentsViewModel;
import com.group09.ComicReader.viewmodel.ReaderViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {
    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_CHAPTER = "extra_chapter";
    private static final long SAVE_PROGRESS_DEBOUNCE_MS = 1500L;
    private static final long RESTORE_RETRY_DELAY_MS = 50L;
    private static final int RESTORE_MAX_ATTEMPTS = 8;
    private static final int RESTORE_OFFSET_TOLERANCE_PX = 24;
    private static final boolean ENABLE_ZOOM_CONTAINER = true;
    private static final float AUDIO_SPEED_075 = 0.75f;
    private static final float AUDIO_SPEED_100 = 1.0f;
    private static final float AUDIO_SPEED_125 = 1.25f;
    private static final int READER_ITEM_VIEW_CACHE_SIZE = 3;

    public static Intent createIntent(@NonNull Context context, int comicId, int chapterId, int chapterNumber) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_COMIC_ID, comicId);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_CHAPTER, chapterNumber);
        return intent;
    }

    public static Intent createDeepLinkIntent(@NonNull Context context, int comicId, int chapterId, int chapterNumber) {
        Intent intent = new Intent(context, ReaderActivity.class);
        if (comicId > 0) {
            intent.putExtra(EXTRA_COMIC_ID, comicId);
        }
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        if (chapterNumber > 0) {
            intent.putExtra(EXTRA_CHAPTER, chapterNumber);
        }
        return intent;
    }

    private ActivityReaderBinding binding;
    private int comicId;
    private int chapterId;
    private int chapterNumber;
    private String comicTitle;
    private ReaderPageAdapter pageAdapter;
    private ReaderViewModel viewModel;
    private ReaderRepository readerRepository;
    private ReaderCommentsFooterAdapter commentsFooterAdapter;
    private CommentsViewModel commentsViewModel;
    private SessionManager sessionManager;
    private boolean historyRecorded;
    private LinearLayoutManager layoutManager;
    private ReaderProgressStore readerProgressStore;
    private ReaderProgressStore.ReaderProgress restoredProgress;
    private boolean restoredPositionApplied;
    private boolean readerZoomed;
    private boolean paywallDialogVisible;
    private ComicChapterResponse currentChapterMeta;
    private ReaderAudioController audioController;
    private int lastRenderedPageIndex = RecyclerView.NO_POSITION;
    private int lastRenderedPageCount = -1;
    private boolean restoringReadingPosition;
    private int missingPageMetadataCount;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveProgressRunnable = this::saveCurrentReadingProgress;
    private final RecyclerView.OnScrollListener progressScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            preloadAroundCurrentViewport();
            if (restoringReadingPosition) {
                return;
            }
            updateReaderProgressUi();
            if (dy != 0) {
                scheduleSaveReadingProgress();
            }
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                preloadAroundCurrentViewport();
                if (restoringReadingPosition) {
                    return;
                }
                scheduleSaveReadingProgress();
                updateReaderProgressUi();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettingsStore settingsStore = new AppSettingsStore(this);
        AppCompatDelegate.setDefaultNightMode(settingsStore.isDarkModeEnabled()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comicId = getIntent().getIntExtra(EXTRA_COMIC_ID, 0);
        chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0);
        chapterNumber = getIntent().getIntExtra(EXTRA_CHAPTER, 0);
        readerProgressStore = new ReaderProgressStore(this);

        ApiClient apiClient = new ApiClient(this);
        readerRepository = new ReaderRepository(this, apiClient);
        ReaderViewModel.Factory factory = new ReaderViewModel.Factory(readerRepository);
        viewModel = new ViewModelProvider(this, factory).get(ReaderViewModel.class);

        audioController = new ReaderAudioController(new ReaderAudioController.Listener() {
            @Override
            public void onPlaybackStateChanged() {
                updateAudioButtonState();
            }

            @Override
            public void onPlaybackError(@NonNull ReaderAudioError error) {
                if (error == ReaderAudioError.MISSING_AUDIO_URL) {
                    Toast.makeText(ReaderActivity.this, R.string.reader_audio_missing_url, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReaderActivity.this, R.string.reader_audio_playback_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }, MediaPlayerReaderAudioPlayer::new);

        pageAdapter = new ReaderPageAdapter();
        commentsFooterAdapter = new ReaderCommentsFooterAdapter(new ReaderCommentsFooterAdapter.Listener() {
            @Override
            public void onSeeMoreClicked() {
                if (commentsViewModel != null) {
                    commentsViewModel.loadMore();
                }
            }

            @Override
            public void onSendComment(@NonNull String text) {
                if (commentsViewModel != null) {
                    commentsViewModel.addComment(text);
                }
            }

            @Override
            public void onReplyToComment(@NonNull com.group09.ComicReader.model.CommentItem comment) {
                if (commentsViewModel != null) {
                    commentsViewModel.startReply(comment);
                }
            }

            @Override
            public void onCancelReply() {
                if (commentsViewModel != null) {
                    commentsViewModel.cancelReply();
                }
            }
        });

        ConcatAdapter concatAdapter = new ConcatAdapter(pageAdapter, commentsFooterAdapter);
        pageAdapter.setItemZoomEnabled(!ENABLE_ZOOM_CONTAINER);
        layoutManager = new LinearLayoutManager(this);
        binding.rcvReaderPages.setLayoutManager(layoutManager);
        binding.rcvReaderPages.setItemViewCacheSize(READER_ITEM_VIEW_CACHE_SIZE);
        binding.rcvReaderPages.setHasFixedSize(false);
        binding.rcvReaderPages.setAdapter(concatAdapter);
        binding.rcvReaderPages.addOnScrollListener(progressScrollListener);
        binding.zoomContainerReader.setZoomEnabled(ENABLE_ZOOM_CONTAINER);
        binding.zoomContainerReader.setOnZoomStateChangeListener((zoomed, scaleFactor) -> {
            readerZoomed = zoomed;
            preloadAroundCurrentViewport();
        });

        setupAudioSpeedControls();

        binding.tvReaderTitle.setText(getString(R.string.app_name));
        if (chapterNumber > 0) {
            binding.tvReaderChapter.setText(getString(R.string.reader_chapter, chapterNumber));
        } else {
            binding.tvReaderChapter.setText("");
        }

        binding.btnReaderShare.setOnClickListener(v -> shareCurrentChapter());

        binding.btnReaderSettings.setOnClickListener(this::showReaderSettingsMenu);
        binding.btnReaderPrevious.setOnClickListener(v -> scrollReaderByPage(-1));
        binding.btnReaderNext.setOnClickListener(v -> scrollReaderByPage(1));

        binding.btnReaderBack.setOnClickListener(v -> {
            resetZoomToBaseState();
            saveCurrentReadingProgress();
            if (audioController != null) {
                audioController.release();
            }
            finish();
        });
        updateReaderProgressUi();
        binding.btnReaderAudio.setOnClickListener(v -> onAudioButtonClicked());

        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        commentsViewModel.getComments().observe(this, comments -> commentsFooterAdapter.setComments(comments));
        commentsViewModel.getHasMore().observe(this, more -> commentsFooterAdapter.setHasMore(more != null && more));
        commentsViewModel.getReplyingToLabel().observe(this, label -> commentsFooterAdapter.setReplyingToLabel(label));

        sessionManager = new SessionManager(this);
        commentsFooterAdapter.setLoggedIn(sessionManager.hasToken());

        commentsViewModel.getPostSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, R.string.comment_posted, Toast.LENGTH_SHORT).show();
            }
        });
        commentsViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                if (ErrorParser.isTokenExpiredMessage(error)) {
                    sessionManager.clear();
                    commentsFooterAdapter.setLoggedIn(false);
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(this, loading -> {
            updateLoadingIndicator();
        });
        viewModel.getPurchaseLoading().observe(this, loading -> updateLoadingIndicator());
        viewModel.getPages().observe(this, pages -> {
            if (!isUiActive()) {
                return;
            }
            List<ReaderPage> safePages = pages == null ? Collections.emptyList() : new ArrayList<>(pages);
            missingPageMetadataCount = ReaderPageAdapter.countPagesWithMissingDimensions(safePages);
            boolean hasStableItemBounds = hasStablePageBounds(safePages);
            binding.rcvReaderPages.setHasFixedSize(hasStableItemBounds);
            pageAdapter.submitList(safePages, () -> {
                if (!isUiActive()) {
                    return;
                }
                boolean restoreStarted = restoreReadingPositionIfNeeded();
                if (!restoreStarted) {
                    binding.rcvReaderPages.post(this::preloadAroundCurrentViewport);
                    binding.rcvReaderPages.post(this::updateReaderProgressUi);
                }
            });
            boolean hasPages = !safePages.isEmpty();
            if (hasPages) {
                paywallDialogVisible = false;
            }
            binding.tvReaderEmpty.setVisibility(hasPages ? android.view.View.GONE : android.view.View.VISIBLE);
            updateReaderProgressUi();
            if (hasPages && comicId > 0 && sessionManager.hasToken() && !historyRecorded) {
                historyRecorded = true;
                viewModel.recordReadingHistory(comicId, chapterId, 1);
            }
        });
        viewModel.getErrorMessage().observe(this, message -> {
            if (!isUiActive()) {
                return;
            }
            if (message != null && !message.trim().isEmpty()) {
                if (ErrorParser.isTokenExpiredMessage(message)) {
                    sessionManager.clear();
                    commentsFooterAdapter.setLoggedIn(false);
                }
                binding.tvReaderEmpty.setVisibility(android.view.View.VISIBLE);
                binding.tvReaderEmpty.setText(message);
                if (isLockedChapterError(message)) {
                    showPurchaseDialogForLockedChapter();
                }
            }
            updateReaderProgressUi();
        });
        viewModel.getPurchaseSuccessBalance().observe(this, balance -> {
            if (!isUiActive()) {
                return;
            }
            if (balance == null) {
                return;
            }
            Toast.makeText(this, getString(R.string.chapter_purchase_success), Toast.LENGTH_SHORT).show();
            paywallDialogVisible = false;
            viewModel.loadChapterPages(chapterId);
        });

        viewModel.getAudioLoading().observe(this, loading -> updateAudioButtonState());
        viewModel.getAudioErrorMessage().observe(this, message -> {
            if (!isUiActive()) {
                return;
            }
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                updateAudioButtonState();
            }
        });
        viewModel.getAudioPages().observe(this, this::onAudioPlaylistReady);
        updateAudioButtonState();

        if (chapterId <= 0) {
            binding.tvReaderEmpty.setText(getString(R.string.reader_invalid_chapter));
            binding.tvReaderEmpty.setVisibility(android.view.View.VISIBLE);
            return;
        }

        resolveChapterMetaIfNeeded();
        initHeaderIfPossible();
        initCommentsIfPossible();
        viewModel.loadChapterPages(chapterId);
    }

    private void resolveChapterMetaIfNeeded() {
        if (readerRepository == null || chapterId <= 0) {
            return;
        }

        readerRepository.getChapterById(chapterId, new ReaderRepository.ChapterCallback() {
            @Override
            public void onSuccess(@NonNull ComicChapterResponse chapter) {
                if (!isUiActive()) {
                    return;
                }
                currentChapterMeta = chapter;
                if (chapter.getComicId() != null && comicId <= 0) {
                    comicId = chapter.getComicId().intValue();
                }
                if (chapter.getChapterNumber() != null && chapterNumber <= 0) {
                    chapterNumber = chapter.getChapterNumber();
                }
                if (binding != null && chapterNumber > 0) {
                    binding.tvReaderChapter.setText(getString(R.string.reader_chapter, chapterNumber));
                }
                initHeaderIfPossible();
                initCommentsIfPossible();
            }

            @Override
            public void onError(@NonNull String message) {
                // Ignore: pages can still load without meta.
            }
        });
    }

    private boolean isLockedChapterError(@NonNull String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("locked") || normalized.contains("purchase it to read");
    }

    private void showPurchaseDialogForLockedChapter() {
        if (paywallDialogVisible) {
            return;
        }
        if (!sessionManager.hasToken()) {
            Toast.makeText(this, getString(R.string.chapter_purchase_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        paywallDialogVisible = true;
        String message = currentChapterMeta != null
                && currentChapterMeta.getPrice() != null
                && currentChapterMeta.getPrice() > 0
                ? getString(R.string.chapter_purchase_confirm_with_price, currentChapterMeta.getPrice())
                : getString(R.string.chapter_purchase_confirm);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chapter_purchase_title))
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(dialog -> paywallDialogVisible = false)
                .setPositiveButton(R.string.chapter_purchase_buy_now, (dialog, which) -> viewModel.purchaseChapter(chapterId))
                .show();
    }

    private void initHeaderIfPossible() {
        if (comicId <= 0) {
            return;
        }
        restoredProgress = readerProgressStore.getProgressForChapter(comicId, chapterId);
        restoreReadingPositionIfNeeded();

        ComicRepository.getInstance().getComicById(comicId, new ComicRepository.ComicCallback() {
            @Override
            public void onSuccess(Comic fetchedComic) {
                if (!isUiActive()) {
                    return;
                }
                if (fetchedComic == null) {
                    return;
                }
                comicTitle = fetchedComic.getTitle();
                if (binding != null && comicTitle != null && !comicTitle.trim().isEmpty()) {
                    binding.tvReaderTitle.setText(comicTitle);
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void initCommentsIfPossible() {
        if (commentsViewModel == null || comicId <= 0) {
            return;
        }
        commentsViewModel.init(comicId, chapterId);
    }

    @Override
    protected void onPause() {
        resetZoomToBaseState();
        saveCurrentReadingProgress();
        if (audioController != null) {
            audioController.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(saveProgressRunnable);
        if (readerRepository != null) {
            readerRepository.cancelAllPendingRequests();
        }
        if (binding != null) {
            binding.rcvReaderPages.removeOnScrollListener(progressScrollListener);
        }
        if (audioController != null) {
            audioController.release();
            audioController = null;
        }
        super.onDestroy();
    }

    private boolean restoreReadingPositionIfNeeded() {
        if (restoredPositionApplied || restoredProgress == null || pageAdapter.getItemCount() == 0) {
            return false;
        }

        int restoredPosition = restoredProgress.getPagePosition();
        if (restoredPosition < 0) {
            return false;
        }
        int maxPosition = Math.max(0, pageAdapter.getItemCount() - 1);
        int targetPosition = Math.min(restoredPosition, maxPosition);
        int targetOffset = restoredProgress.getOffset();
        boolean hasMissingMetadata = missingPageMetadataCount > 0;
        restoringReadingPosition = true;
        attemptRestoreReadingPosition(targetPosition, targetOffset, 1, !hasMissingMetadata, hasMissingMetadata);
        return true;
    }

    private void attemptRestoreReadingPosition(
            int targetPosition,
            int targetOffset,
            int attempt,
            boolean applySavedOffset,
            boolean metadataIncomplete) {
        if (!isUiActive() || layoutManager == null || binding == null) {
            restoringReadingPosition = false;
            return;
        }
        int requestedOffset = applySavedOffset ? targetOffset : 0;
        layoutManager.scrollToPositionWithOffset(targetPosition, requestedOffset);
        binding.rcvReaderPages.post(() -> {
            if (!isUiActive() || layoutManager == null) {
                restoringReadingPosition = false;
                return;
            }
            View targetView = layoutManager.findViewByPosition(targetPosition);
            boolean targetReady = targetView != null && targetView.getHeight() > 0;
            int actualTop = targetView == null ? Integer.MIN_VALUE : targetView.getTop();
            boolean offsetApplied = targetView != null
                    && Math.abs(actualTop - requestedOffset) <= RESTORE_OFFSET_TOLERANCE_PX;

            if (targetReady && applySavedOffset && offsetApplied) {
                finishRestoreReadingPosition(true, targetPosition, targetOffset, actualTop, attempt, metadataIncomplete);
                return;
            }
            if (targetReady && !applySavedOffset) {
                scheduleRestoreRetry(targetPosition, targetOffset, attempt + 1, true, metadataIncomplete);
                return;
            }
            if (attempt >= RESTORE_MAX_ATTEMPTS) {
                finishRestoreReadingPosition(false, targetPosition, targetOffset, actualTop, attempt, metadataIncomplete);
                return;
            }
            scheduleRestoreRetry(targetPosition, targetOffset, attempt + 1, applySavedOffset, metadataIncomplete);
        });
    }

    private void scheduleRestoreRetry(
            int targetPosition,
            int targetOffset,
            int nextAttempt,
            boolean applySavedOffset,
            boolean metadataIncomplete) {
        if (binding == null) {
            restoringReadingPosition = false;
            return;
        }
        binding.rcvReaderPages.postDelayed(
                () -> attemptRestoreReadingPosition(
                        targetPosition,
                        targetOffset,
                        nextAttempt,
                        applySavedOffset,
                        metadataIncomplete),
                RESTORE_RETRY_DELAY_MS);
    }

    private void finishRestoreReadingPosition(
            boolean success,
            int targetPosition,
            int targetOffset,
            int actualTop,
            int attempts,
            boolean metadataIncomplete) {
        restoredPositionApplied = true;
        restoringReadingPosition = false;
        preloadAroundCurrentViewport();
        updateReaderProgressUi();
    }

    private void scheduleSaveReadingProgress() {
        if (restoringReadingPosition) {
            return;
        }
        progressHandler.removeCallbacks(saveProgressRunnable);
        progressHandler.postDelayed(saveProgressRunnable, SAVE_PROGRESS_DEBOUNCE_MS);
    }

    private void saveCurrentReadingProgress() {
        if (comicId <= 0 || chapterId <= 0 || chapterNumber <= 0) {
            return;
        }
        if (layoutManager == null || pageAdapter == null) {
            return;
        }
        int pageCount = pageAdapter.getItemCount();
        if (pageCount <= 0) {
            return;
        }
        int firstVisibleAdapterPosition = layoutManager.findFirstVisibleItemPosition();
        int pagePosition = ReaderProgressPositionResolver.resolvePagePosition(firstVisibleAdapterPosition, pageCount);
        if (pagePosition < 0) {
            return;
        }
        View firstVisiblePageView = layoutManager.findViewByPosition(pagePosition);
        int offset = firstVisiblePageView == null ? 0 : firstVisiblePageView.getTop();
        readerProgressStore.saveProgress(comicId, chapterId, chapterNumber, pagePosition, offset);
    }

    private void shareCurrentChapter() {
        if (chapterId <= 0) {
            return;
        }
        String url = ApiClient.toAbsolutePublicUrl("/share/chapter/" + chapterId);

        String shareText = url;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_chooser_title)));
    }

    private void resetZoomToBaseState() {
        if (ENABLE_ZOOM_CONTAINER && binding != null) {
            binding.zoomContainerReader.resetZoom();
        }
    }

    private void onAudioButtonClicked() {
        Boolean loading = viewModel.getAudioLoading().getValue();
        if (loading != null && loading) {
            return;
        }
        if (audioController == null) {
            return;
        }

        if (!audioController.hasPlaylist()) {
            viewModel.createOrGetChapterAudioPlaylist(chapterId);
            updateAudioButtonState();
            return;
        }

        if (audioController.isPlaying()) {
            audioController.pause();
            return;
        }

        audioController.playOrResume();
    }

    private void onAudioPlaylistReady(List<ReaderAudioPage> pages) {
        if (!isUiActive() || audioController == null) {
            return;
        }
        List<ReaderAudioPage> safePages = pages == null ? Collections.emptyList() : new ArrayList<>(pages);
        audioController.setPlaylist(safePages);

        if (safePages.isEmpty()) {
            Toast.makeText(this, R.string.reader_audio_no_content, Toast.LENGTH_SHORT).show();
            updateAudioButtonState();
            return;
        }

        audioController.playOrResume();
    }

    private void updateAudioButtonState() {
        if (binding == null) {
            return;
        }
        if (audioController == null) {
            binding.btnReaderAudio.setEnabled(false);
            binding.btnReaderAudio.setText(R.string.reader_audio_request);
            return;
        }

        Boolean loading = viewModel.getAudioLoading().getValue();
        if (loading != null && loading) {
            binding.btnReaderAudio.setEnabled(false);
            binding.btnReaderAudio.setText(R.string.reader_audio_loading);
            return;
        }

        binding.btnReaderAudio.setEnabled(true);
        if (!audioController.hasPlaylist()) {
            binding.btnReaderAudio.setText(R.string.reader_audio_request);
            return;
        }

        if (audioController.isPlaying()) {
            binding.btnReaderAudio.setText(R.string.reader_audio_pause);
            return;
        }

        binding.btnReaderAudio.setText(R.string.reader_audio_play);
    }

    private void setupAudioSpeedControls() {
        binding.toggleReaderAudioSpeed.check(R.id.btn_reader_audio_speed_100);
        binding.toggleReaderAudioSpeed.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            audioController.setPlaybackSpeed(resolveSpeedByButtonId(checkedId));
        });
    }

    private float resolveSpeedByButtonId(int buttonId) {
        if (buttonId == R.id.btn_reader_audio_speed_075) {
            return AUDIO_SPEED_075;
        }
        if (buttonId == R.id.btn_reader_audio_speed_125) {
            return AUDIO_SPEED_125;
        }
        return AUDIO_SPEED_100;
    }

    private void preloadAroundCurrentViewport() {
        if (layoutManager == null || pageAdapter == null) {
            return;
        }
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        pageAdapter.preloadAroundVisibleRange(firstVisible, lastVisible, readerZoomed);
    }

    private void showReaderSettingsMenu(@NonNull View anchorView) {
        PopupMenu menu = new PopupMenu(this, anchorView);
        menu.getMenu().add(Menu.NONE, 1, 1, getString(R.string.reader_action_reset_zoom));
        menu.getMenu().add(Menu.NONE, 2, 2, getString(R.string.reader_action_scroll_top));
        menu.getMenu().add(Menu.NONE, 3, 3, getString(R.string.reader_action_jump_to_comments));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                resetZoomToBaseState();
                return true;
            }
            if (item.getItemId() == 2) {
                if (layoutManager != null) {
                    binding.rcvReaderPages.smoothScrollToPosition(0);
                }
                return true;
            }
            if (item.getItemId() == 3) {
                scrollToCommentsSection();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void scrollReaderByPage(int delta) {
        if (layoutManager == null || pageAdapter == null) {
            return;
        }
        int pageCount = pageAdapter.getItemCount();
        if (pageCount <= 0) {
            return;
        }
        int currentIndex = getCurrentReaderPageIndex();
        int targetIndex = Math.max(0, Math.min(pageCount - 1, currentIndex + delta));
        if (targetIndex == currentIndex) {
            return;
        }
        resetZoomToBaseState();
        // scrollToPositionWithOffset on the LayoutManager snaps target item to screen top (offset=0).
        // smoothScrollToPosition only scrolls until item is barely visible — insufficient
        // when page images are taller than the screen, causing the "partial scroll then stuck" bug.
        layoutManager.scrollToPositionWithOffset(targetIndex, 0);
        binding.rcvReaderPages.post(() -> {
            updateReaderProgressUi();
            preloadAroundCurrentViewport();
        });
    }

    private void scrollToCommentsSection() {
        if (pageAdapter == null) {
            return;
        }
        int footerPosition = pageAdapter.getItemCount();
        if (footerPosition < 0) {
            return;
        }
        binding.rcvReaderPages.smoothScrollToPosition(footerPosition);
    }

    private int getCurrentReaderPageIndex() {
        if (layoutManager == null || pageAdapter == null || pageAdapter.getItemCount() <= 0) {
            return 0;
        }
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        if (firstVisible == RecyclerView.NO_POSITION) {
            return 0;
        }
        int lastPageIndex = pageAdapter.getItemCount() - 1;
        return Math.max(0, Math.min(firstVisible, lastPageIndex));
    }

    private void updateReaderProgressUi() {
        if (binding == null || pageAdapter == null) {
            return;
        }
        int pageCount = pageAdapter.getItemCount();
        int currentPage = pageCount > 0 ? getCurrentReaderPageIndex() + 1 : 0;
        int currentPageIndex = currentPage > 0 ? currentPage - 1 : RecyclerView.NO_POSITION;
        if (currentPageIndex == lastRenderedPageIndex && pageCount == lastRenderedPageCount) {
            return;
        }
        lastRenderedPageIndex = currentPageIndex;
        lastRenderedPageCount = pageCount;

        binding.tvReaderProgressStart.setText(String.valueOf(currentPage));
        binding.tvReaderProgressEnd.setText(String.valueOf(pageCount));
        binding.pbReaderProgress.setMax(Math.max(1, pageCount));
        binding.pbReaderProgress.setProgressCompat(currentPage, true);

        boolean canGoPrevious = pageCount > 0 && currentPage > 1;
        boolean canGoNext = pageCount > 0 && currentPage < pageCount;
        binding.btnReaderPrevious.setEnabled(canGoPrevious);
        binding.btnReaderNext.setEnabled(canGoNext);
        binding.btnReaderPrevious.setAlpha(canGoPrevious ? 1f : 0.5f);
        binding.btnReaderNext.setAlpha(canGoNext ? 1f : 0.5f);
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    private void updateLoadingIndicator() {
        boolean pageLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean purchaseLoading = Boolean.TRUE.equals(viewModel.getPurchaseLoading().getValue());
        binding.prgReaderLoading.setVisibility(pageLoading || purchaseLoading ? View.VISIBLE : View.GONE);
    }

    private boolean hasStablePageBounds(@NonNull List<ReaderPage> pages) {
        if (pages.isEmpty()) {
            return false;
        }
        return ReaderPageAdapter.countPagesWithMissingDimensions(pages) == 0;
    }
}
