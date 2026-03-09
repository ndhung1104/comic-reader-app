package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ReaderPageAdapter;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comicId = getIntent().getIntExtra(EXTRA_COMIC_ID, 1);
        chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 1);
        chapterNumber = getIntent().getIntExtra(EXTRA_CHAPTER, 1);

        Comic comic = ComicRepository.getInstance().getComicById(comicId);
        String title = comic == null ? getString(R.string.app_name) : comic.getTitle();

        ApiClient apiClient = new ApiClient(this);
        ReaderRepository readerRepository = new ReaderRepository(apiClient);
        ReaderViewModel.Factory factory = new ReaderViewModel.Factory(readerRepository);
        viewModel = new ViewModelProvider(this, factory).get(ReaderViewModel.class);

        pageAdapter = new ReaderPageAdapter();
        binding.rcvReaderPages.setLayoutManager(new LinearLayoutManager(this));
        binding.rcvReaderPages.setAdapter(pageAdapter);

        binding.tvReaderTitle.setText(title);
        binding.tvReaderChapter.setText(getString(R.string.reader_chapter, chapterNumber));

        binding.btnReaderBack.setOnClickListener(v -> finish());

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
