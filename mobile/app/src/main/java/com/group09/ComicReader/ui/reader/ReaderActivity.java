package com.group09.ComicReader.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.databinding.ActivityReaderBinding;
import com.group09.ComicReader.model.Comic;

public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_CHAPTER = "extra_chapter";

    public static Intent createIntent(@NonNull Context context, int comicId, int chapter) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_COMIC_ID, comicId);
        intent.putExtra(EXTRA_CHAPTER, chapter);
        return intent;
    }

    private ActivityReaderBinding binding;
    private int comicId;
    private int chapter;
    private int currentPage = 1;
    private final int totalPages = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comicId = getIntent().getIntExtra(EXTRA_COMIC_ID, 1);
        chapter = getIntent().getIntExtra(EXTRA_CHAPTER, 1);

        Comic comic = ComicRepository.getInstance().getComicById(comicId);
        String title = comic == null ? getString(R.string.app_name) : comic.getTitle();

        binding.tvReaderTitle.setText(title);
        binding.tvReaderChapter.setText("Chapter " + chapter);
        binding.tvReaderTotalPages.setText(String.valueOf(totalPages));
        binding.sldReaderPage.setValueFrom(1f);
        binding.sldReaderPage.setValueTo(totalPages);
        binding.sldReaderPage.setStepSize(1f);
        binding.sldReaderPage.setValue(currentPage);

        if (comic != null) {
            Glide.with(this)
                    .load(comic.getCoverUrl())
                    .into(binding.imgReaderComicPage);
        }

        updatePageText();

        binding.btnReaderBack.setOnClickListener(v -> finish());
        binding.btnReaderPrevious.setOnClickListener(v -> {
            if (chapter > 1) {
                chapter -= 1;
                binding.tvReaderChapter.setText("Chapter " + chapter);
                updateChapterButtons();
            }
        });
        binding.btnReaderNext.setOnClickListener(v -> {
            chapter += 1;
            binding.tvReaderChapter.setText("Chapter " + chapter);
            updateChapterButtons();
        });
        binding.sldReaderPage.addOnChangeListener((slider, value, fromUser) -> {
            currentPage = Math.round(value);
            updatePageText();
        });

        binding.clReaderRoot.setOnClickListener(v -> toggleControls());
        updateChapterButtons();
    }

    private void toggleControls() {
        int nextVisibility = binding.clReaderTopBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        binding.clReaderTopBar.setVisibility(nextVisibility);
        binding.clReaderBottomBar.setVisibility(nextVisibility);
    }

    private void updatePageText() {
        binding.tvReaderPage.setText(getString(R.string.reader_page, currentPage));
    }

    private void updateChapterButtons() {
        binding.btnReaderPrevious.setEnabled(chapter > 1);
    }
}
