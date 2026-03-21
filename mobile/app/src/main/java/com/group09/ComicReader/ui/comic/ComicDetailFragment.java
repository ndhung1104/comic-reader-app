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
import com.group09.ComicReader.adapter.CommentAdapter;
import com.group09.ComicReader.adapter.RelatedComicAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentComicDetailBinding;
import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.ui.reader.ReaderActivity;
import com.group09.ComicReader.viewmodel.ComicDetailViewModel;
import com.group09.ComicReader.viewmodel.CommentsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComicDetailFragment extends BaseFragment {

    private FragmentComicDetailBinding binding;
    private ComicDetailViewModel viewModel;
    private ChapterAdapter chapterAdapter;
    private RelatedComicAdapter relatedComicAdapter;
    private CommentAdapter commentsAdapter;
    private CommentsViewModel commentsViewModel;
    private SessionManager sessionManager;
    private int comicId;
    private Comic currentComic;
    private List<Chapter> currentChapters = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentComicDetailBinding.inflate(inflater, container, false);
        ApiClient apiClient = new ApiClient(requireContext());
        ComicRepository comicRepository = ComicRepository.getInstance();
        ReaderRepository readerRepository = new ReaderRepository(apiClient);
        ComicDetailViewModel.Factory factory =
                new ComicDetailViewModel.Factory(comicRepository, readerRepository);
        viewModel = new ViewModelProvider(this, factory).get(ComicDetailViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        comicId = ComicDetailFragmentArgs.fromBundle(requireArguments()).getComicId();

        sessionManager = new SessionManager(requireContext());
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);

        chapterAdapter = new ChapterAdapter(this::openReaderForChapter);
        binding.rcvComicDetailChapters.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvComicDetailChapters.setAdapter(chapterAdapter);

        relatedComicAdapter = new RelatedComicAdapter(this::openRelatedComic);
        binding.rcvComicDetailRelated.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rcvComicDetailRelated.setAdapter(relatedComicAdapter);

        binding.btnComicDetailBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        binding.btnComicDetailDownload.setOnClickListener(v -> showToast("Download is not implemented"));
        binding.btnComicDetailRead.setOnClickListener(v -> openFirstAvailableChapter());
        binding.btnComicDetailComments.setOnClickListener(v -> scrollToCommentsFooter());

        setupCommentsFooter();
        commentsViewModel.init(comicId, null);

        observeData();
        viewModel.loadData(comicId);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCommentsFooterLoginState();
    }

    private void setupCommentsFooter() {
        commentsAdapter = new CommentAdapter();
        binding.incComicDetailCommentsFooter.rcvReaderComments.setAdapter(commentsAdapter);

        binding.incComicDetailCommentsFooter.tvReaderCommentsSeeMore.setOnClickListener(v ->
                commentsViewModel.loadMore());

        binding.incComicDetailCommentsFooter.tilReaderCommentsInput.setEndIconOnClickListener(v ->
                submitOutsideComment());

        commentsViewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            commentsAdapter.submitList(comments);

            boolean hasComments = comments != null && !comments.isEmpty();
            binding.incComicDetailCommentsFooter.rcvReaderComments.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            binding.incComicDetailCommentsFooter.tvReaderCommentsEmpty.setVisibility(hasComments ? View.GONE : View.VISIBLE);
        });

        commentsViewModel.getHasMore().observe(getViewLifecycleOwner(), more -> {
            boolean show = more != null && more;
            binding.incComicDetailCommentsFooter.tvReaderCommentsSeeMore.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        commentsViewModel.getPostSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                showToast(getString(com.group09.ComicReader.R.string.comment_posted));
                binding.incComicDetailCommentsFooter.edtReaderCommentsInput.setText("");
                binding.incComicDetailCommentsFooter.rcvReaderComments.scrollToPosition(0);
            }
        });

        commentsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                if ("Session expired. Please log in again.".equals(error)) {
                    sessionManager.clear();
                    updateCommentsFooterLoginState();
                }
                showToast(error);
            }
        });

        updateCommentsFooterLoginState();
    }

    private void updateCommentsFooterLoginState() {
        boolean isLoggedIn = sessionManager != null && sessionManager.hasToken();

        binding.incComicDetailCommentsFooter.edtReaderCommentsInput.setEnabled(isLoggedIn);
        binding.incComicDetailCommentsFooter.tilReaderCommentsInput.setHint(isLoggedIn
                ? getString(com.group09.ComicReader.R.string.comment_hint)
                : getString(com.group09.ComicReader.R.string.comment_login_required));
    }

    private void submitOutsideComment() {
        boolean isLoggedIn = sessionManager != null && sessionManager.hasToken();
        if (!isLoggedIn) {
            showToast(getString(com.group09.ComicReader.R.string.comment_login_required));
            return;
        }

        String text = binding.incComicDetailCommentsFooter.edtReaderCommentsInput.getText() == null
                ? "" : binding.incComicDetailCommentsFooter.edtReaderCommentsInput.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }

        // Outside comments => chapterId = null
        commentsViewModel.addComment(text.trim());
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
        viewModel.getChapters().observe(getViewLifecycleOwner(), chapters -> {
            currentChapters = chapters == null ? new ArrayList<>() : chapters;
            chapterAdapter.submitList(chapters);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });
        viewModel.getRelatedComics().observe(getViewLifecycleOwner(), comics -> {
            relatedComicAdapter.submitList(comics);
            boolean hasRelated = comics != null && !comics.isEmpty();
            binding.tvComicDetailRelatedLabel.setVisibility(hasRelated ? View.VISIBLE : View.GONE);
            binding.rcvComicDetailRelated.setVisibility(hasRelated ? View.VISIBLE : View.GONE);
        });
    }

    private void openReaderForChapter(Chapter chapter) {
        if (chapter == null) {
            return;
        }
        if (!chapter.isUnlocked()) {
            showToast(getString(com.group09.ComicReader.R.string.chapter_locked_message));
            return;
        }
        openReader(chapter.getId(), chapter.getNumber());
    }

    private void openFirstAvailableChapter() {
        for (Chapter chapter : currentChapters) {
            if (chapter != null && chapter.isUnlocked()) {
                openReader(chapter.getId(), chapter.getNumber());
                return;
            }
        }
        showToast(getString(com.group09.ComicReader.R.string.comic_no_unlocked_chapter));
    }

    private void openReader(int chapterId, int chapterNumber) {
        if (currentComic == null) {
            return;
        }
        Intent intent = ReaderActivity.createIntent(requireContext(), currentComic.getId(), chapterId, chapterNumber);
        startActivity(intent);
    }

    private void openRelatedComic(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        ComicDetailFragmentArgs args = new ComicDetailFragmentArgs.Builder(comic.getId()).build();
        Navigation.findNavController(getView()).navigate(
                com.group09.ComicReader.R.id.comicDetailFragment, args.toBundle());
    }

    private void scrollToCommentsFooter() {
        if (binding == null) {
            return;
        }
        binding.nsvComicDetailContent.post(() -> {
            if (binding == null) {
                return;
            }
            View footer = binding.incComicDetailCommentsFooter.getRoot();
            binding.nsvComicDetailContent.smoothScrollTo(0, footer.getTop());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
