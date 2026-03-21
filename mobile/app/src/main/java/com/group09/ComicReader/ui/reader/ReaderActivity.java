package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.widget.Toast;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ReaderPageAdapter;
import com.group09.ComicReader.adapter.ReaderCommentsFooterAdapter;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.CommentsViewModel;
import com.group09.ComicReader.viewmodel.ReaderViewModel;

public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_CHAPTER = "extra_chapter";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comicId = getIntent().getIntExtra(EXTRA_COMIC_ID, 1);
        chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 1);
        chapterNumber = getIntent().getIntExtra(EXTRA_CHAPTER, 1);

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
        binding.rcvReaderPages.setLayoutManager(new LinearLayoutManager(this));
        binding.rcvReaderPages.setAdapter(concatAdapter);

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

        binding.btnReaderBack.setOnClickListener(v -> finish());

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
            pageAdapter.submitList(pages);
            boolean hasPages = pages != null && !pages.isEmpty();
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
}
