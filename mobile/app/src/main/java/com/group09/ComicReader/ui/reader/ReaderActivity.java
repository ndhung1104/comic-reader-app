package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ReaderCommentsFooterAdapter;
import com.group09.ComicReader.adapter.ReaderPageAdapter;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.ReaderProgressStore;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.ReaderAudioPage;
import com.group09.ComicReader.model.ReaderPage;
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
    private static final boolean ENABLE_ZOOM_CONTAINER = true;

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
    private ReaderCommentsFooterAdapter commentsFooterAdapter;
    private CommentsViewModel commentsViewModel;
    private boolean historyRecorded;
    private LinearLayoutManager layoutManager;
    private ReaderProgressStore readerProgressStore;
    private ReaderProgressStore.ReaderProgress restoredProgress;
    private boolean restoredPositionApplied;
    private boolean readerZoomed;
    private final List<ReaderAudioPage> audioPlaylist = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentAudioIndex = -1;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveProgressRunnable = this::saveCurrentReadingProgress;
    private final RecyclerView.OnScrollListener progressScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            preloadAroundCurrentViewport();
            if (dy != 0) {
                scheduleSaveReadingProgress();
            }
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                preloadAroundCurrentViewport();
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
        binding.rcvReaderPages.setItemViewCacheSize(6);
        binding.rcvReaderPages.setAdapter(concatAdapter);
        binding.rcvReaderPages.addOnScrollListener(progressScrollListener);
        binding.zoomContainerReader.setZoomEnabled(ENABLE_ZOOM_CONTAINER);
        binding.zoomContainerReader.setOnZoomStateChangeListener((zoomed, scaleFactor) -> {
            readerZoomed = zoomed;
            preloadAroundCurrentViewport();
        });

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
            resetZoomToBaseState();
            saveCurrentReadingProgress();
            releaseMediaPlayer();
            finish();
        });
        binding.btnReaderAudio.setOnClickListener(v -> onAudioButtonClicked());

        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        commentsViewModel.init(comicId, chapterId);
        commentsViewModel.getComments().observe(this, comments -> commentsFooterAdapter.setComments(comments));
        commentsViewModel.getHasMore().observe(this, more -> commentsFooterAdapter.setHasMore(more != null && more));
        commentsViewModel.getReplyingToLabel().observe(this, label -> commentsFooterAdapter.setReplyingToLabel(label));

        SessionManager sessionManager = new SessionManager(this);
        commentsFooterAdapter.setLoggedIn(sessionManager.hasToken());

        commentsViewModel.getPostSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, R.string.comment_posted, Toast.LENGTH_SHORT).show();
            }
        });
        commentsViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                if ("Session expired. Please log in again.".equals(error)) {
                    sessionManager.clear();
                    commentsFooterAdapter.setLoggedIn(false);
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            binding.prgReaderLoading.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        viewModel.getPages().observe(this, pages -> {
            List<ReaderPage> safePages = pages == null ? Collections.emptyList() : new ArrayList<>(pages);
            pageAdapter.submitList(safePages, () -> {
                restoreReadingPositionIfNeeded();
                binding.rcvReaderPages.post(this::preloadAroundCurrentViewport);
            });
            boolean hasPages = !safePages.isEmpty();
            binding.tvReaderEmpty.setVisibility(hasPages ? android.view.View.GONE : android.view.View.VISIBLE);
            if (hasPages && sessionManager.hasToken() && !historyRecorded) {
                historyRecorded = true;
                viewModel.recordReadingHistory(comicId, chapterId, 1);
            }
        });
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                if ("Session expired. Please log in again.".equals(message)) {
                    sessionManager.clear();
                    commentsFooterAdapter.setLoggedIn(false);
                }
                binding.tvReaderEmpty.setVisibility(android.view.View.VISIBLE);
                binding.tvReaderEmpty.setText(message);
            }
        });

        viewModel.getAudioLoading().observe(this, loading -> updateAudioButtonState());
        viewModel.getAudioErrorMessage().observe(this, message -> {
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
        viewModel.loadChapterPages(chapterId);
    }

    @Override
    protected void onPause() {
        resetZoomToBaseState();
        saveCurrentReadingProgress();
        pauseMediaIfPlaying();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(saveProgressRunnable);
        if (binding != null) {
            binding.rcvReaderPages.removeOnScrollListener(progressScrollListener);
        }
        releaseMediaPlayer();
        super.onDestroy();
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

        binding.rcvReaderPages.post(() -> {
            layoutManager.scrollToPositionWithOffset(targetPosition, targetOffset);
            binding.rcvReaderPages.post(this::preloadAroundCurrentViewport);
        });
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

        if (audioPlaylist.isEmpty()) {
            viewModel.createOrGetChapterAudioPlaylist(chapterId);
            updateAudioButtonState();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateAudioButtonState();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.start();
            updateAudioButtonState();
            return;
        }

        if (currentAudioIndex < 0 || currentAudioIndex >= audioPlaylist.size()) {
            currentAudioIndex = 0;
        }
        playAudioAtIndex(currentAudioIndex);
    }

    private void onAudioPlaylistReady(List<ReaderAudioPage> pages) {
        audioPlaylist.clear();
        if (pages != null) {
            audioPlaylist.addAll(pages);
        }

        if (audioPlaylist.isEmpty()) {
            Toast.makeText(this, R.string.reader_audio_no_content, Toast.LENGTH_SHORT).show();
            currentAudioIndex = -1;
            updateAudioButtonState();
            return;
        }

        currentAudioIndex = 0;
        playAudioAtIndex(currentAudioIndex);
    }

    private void playAudioAtIndex(int index) {
        if (index < 0 || index >= audioPlaylist.size()) {
            releaseMediaPlayer();
            updateAudioButtonState();
            return;
        }

        ReaderAudioPage page = audioPlaylist.get(index);
        if (page.getAudioUrl() == null || page.getAudioUrl().trim().isEmpty()) {
            Toast.makeText(this, R.string.reader_audio_missing_url, Toast.LENGTH_SHORT).show();
            updateAudioButtonState();
            return;
        }

        releaseMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );
            mediaPlayer.setDataSource(page.getAudioUrl());
            mediaPlayer.setOnPreparedListener(player -> {
                player.start();
                updateAudioButtonState();
            });
            mediaPlayer.setOnCompletionListener(player -> {
                int nextIndex = currentAudioIndex + 1;
                if (nextIndex < audioPlaylist.size()) {
                    currentAudioIndex = nextIndex;
                    playAudioAtIndex(nextIndex);
                    return;
                }
                currentAudioIndex = 0;
                releaseMediaPlayer();
                updateAudioButtonState();
            });
            mediaPlayer.setOnErrorListener((player, what, extra) -> {
                Toast.makeText(this, R.string.reader_audio_playback_failed, Toast.LENGTH_SHORT).show();
                releaseMediaPlayer();
                updateAudioButtonState();
                return true;
            });
            mediaPlayer.prepareAsync();
            updateAudioButtonState();
        } catch (Exception exception) {
            releaseMediaPlayer();
            Toast.makeText(this, R.string.reader_audio_playback_failed, Toast.LENGTH_SHORT).show();
            updateAudioButtonState();
        }
    }

    private void pauseMediaIfPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateAudioButtonState();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (Exception ignored) {
        }
        mediaPlayer = null;
    }

    private void updateAudioButtonState() {
        if (binding == null) {
            return;
        }

        Boolean loading = viewModel.getAudioLoading().getValue();
        if (loading != null && loading) {
            binding.btnReaderAudio.setEnabled(false);
            binding.btnReaderAudio.setText(R.string.reader_audio_loading);
            return;
        }

        binding.btnReaderAudio.setEnabled(true);
        if (audioPlaylist.isEmpty()) {
            binding.btnReaderAudio.setText(R.string.reader_audio_request);
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            binding.btnReaderAudio.setText(R.string.reader_audio_pause);
            return;
        }

        binding.btnReaderAudio.setText(R.string.reader_audio_play);
    }

    private void preloadAroundCurrentViewport() {
        if (layoutManager == null || pageAdapter == null) {
            return;
        }
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        pageAdapter.preloadAroundVisibleRange(firstVisible, lastVisible, readerZoomed);
    }
}
