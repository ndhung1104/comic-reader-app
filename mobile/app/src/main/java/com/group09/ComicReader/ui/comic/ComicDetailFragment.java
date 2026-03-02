package com.group09.ComicReader.ui.comic;

import android.content.Intent;
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
import com.group09.ComicReader.adapter.ChapterAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentComicDetailBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.ui.comment.CommentSheetBottomSheetFragment;
import com.group09.ComicReader.ui.reader.ReaderActivity;
import com.group09.ComicReader.viewmodel.ComicDetailViewModel;

import java.util.Locale;

public class ComicDetailFragment extends BaseFragment {

    private FragmentComicDetailBinding binding;
    private ComicDetailViewModel viewModel;
    private ChapterAdapter chapterAdapter;
    private int comicId;
    private Comic currentComic;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentComicDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ComicDetailViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        comicId = ComicDetailFragmentArgs.fromBundle(requireArguments()).getComicId();

        chapterAdapter = new ChapterAdapter(chapter -> openReader(chapter.getNumber()));
        binding.rcvComicDetailChapters.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvComicDetailChapters.setAdapter(chapterAdapter);

        binding.btnComicDetailBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        binding.btnComicDetailDownload.setOnClickListener(v -> showToast("Download is not implemented"));
        binding.btnComicDetailRead.setOnClickListener(v -> openReader(1));
        binding.btnComicDetailComments.setOnClickListener(v -> openCommentsSheet());

        observeData();
        viewModel.loadData(comicId);
    }

    private void observeData() {
        viewModel.getComic().observe(getViewLifecycleOwner(), comic -> {
            currentComic = comic;
            if (comic == null) {
                binding.tvComicDetailTitle.setText(com.group09.ComicReader.R.string.comic_not_found);
                return;
            }
            binding.tvComicDetailTitle.setText(comic.getTitle());
            binding.tvComicDetailAuthor.setText(comic.getAuthor());
            binding.tvComicDetailRating.setText(String.format(Locale.US, "%.1f", comic.getRating()));
            binding.tvComicDetailGenres.setText(comic.getGenres().isEmpty() ? "" : String.join(", ", comic.getGenres()));
            binding.tvComicDetailSynopsis.setText(comic.getSynopsis());
            Glide.with(binding.imgComicDetailCover)
                    .load(comic.getCoverUrl())
                    .into(binding.imgComicDetailCover);
            Glide.with(binding.imgComicDetailBackground)
                    .load(comic.getCoverUrl())
                    .into(binding.imgComicDetailBackground);
        });
        viewModel.getChapters().observe(getViewLifecycleOwner(), chapterAdapter::submitList);
    }

    private void openReader(int chapter) {
        if (currentComic == null) {
            return;
        }
        Intent intent = ReaderActivity.createIntent(requireContext(), currentComic.getId(), chapter);
        startActivity(intent);
    }

    private void openCommentsSheet() {
        CommentSheetBottomSheetFragment bottomSheet = CommentSheetBottomSheetFragment.newInstance(comicId);
        bottomSheet.show(getChildFragmentManager(), "comment_sheet");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
