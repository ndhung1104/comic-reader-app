package com.group09.ComicReader.ui.comic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.ChapterAdapter;
import com.group09.ComicReader.adapter.CommentAdapter;
import com.group09.ComicReader.adapter.RelatedComicAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.ComicRepository;
import com.group09.ComicReader.data.LibraryRepository;
import com.group09.ComicReader.data.ReaderRepository;
import com.group09.ComicReader.data.local.ReaderProgressStore;
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
    private ReaderProgressStore readerProgressStore;
    private int comicId;
    private int pendingPurchaseChapterId = -1;
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
        LibraryRepository libraryRepository = new LibraryRepository(apiClient);
        ComicDetailViewModel.Factory factory =
                new ComicDetailViewModel.Factory(comicRepository, readerRepository, libraryRepository);
        viewModel = new ViewModelProvider(this, factory).get(ComicDetailViewModel.class);
        readerProgressStore = new ReaderProgressStore(requireContext());
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
        binding.btnComicDetailShare.setOnClickListener(v -> shareComicDeepLink());
        binding.btnComicDetailRead.setOnClickListener(v -> openFirstAvailableChapter());
        binding.btnComicDetailComments.setOnClickListener(v -> scrollToCommentsFooter());
        binding.btnComicDetailFollow.setOnClickListener(v -> toggleFollow());
        binding.btnComicDetailTranslate.setOnClickListener(v -> viewModel.translateComic(comicId, "vi"));
        binding.btnComicDetailRate.setOnClickListener(v -> showRatingDialog());

        setupCommentsFooter();
        commentsViewModel.init(comicId, null);

        observeData();
        viewModel.loadData(comicId);
        if (sessionManager.hasToken()) {
            viewModel.loadFollowStatus(comicId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCommentsFooterLoginState();
        if (comicId > 0) {
            viewModel.loadData(comicId);
        }
    }

    private void setupCommentsFooter() {
        commentsAdapter = new CommentAdapter(comment -> {
            boolean isLoggedIn = sessionManager != null && sessionManager.hasToken();
            if (!isLoggedIn) {
                showToast(getString(com.group09.ComicReader.R.string.comment_login_required));
                return;
            }
            commentsViewModel.startReply(comment);
            binding.nsvComicDetailContent.post(() -> {
                if (binding == null) return;
                View footer = binding.incComicDetailCommentsFooter.getRoot();
                binding.nsvComicDetailContent.smoothScrollTo(0, footer.getTop());
                binding.incComicDetailCommentsFooter.edtReaderCommentsInput.requestFocus();
            });
        });
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
            }
        });

        commentsViewModel.getReplyingToLabel().observe(getViewLifecycleOwner(), label -> {
            boolean isReplying = label != null && !label.trim().isEmpty();
            binding.incComicDetailCommentsFooter.clReaderCommentsReplying.setVisibility(isReplying ? View.VISIBLE : View.GONE);
            if (isReplying) {
                binding.incComicDetailCommentsFooter.tvReaderCommentsReplyingTo.setText(label);
            }
        });

        binding.incComicDetailCommentsFooter.tvReaderCommentsCancelReply.setOnClickListener(v ->
                commentsViewModel.cancelReply());

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
            binding.tvComicDetailViewCount.setText(formatViewCount(comic.getViewCount()));
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
        viewModel.getPurchasedChapter().observe(getViewLifecycleOwner(), chapter -> {
            if (chapter == null) {
                return;
            }
            if (pendingPurchaseChapterId > 0 && chapter.getId() == pendingPurchaseChapterId) {
                pendingPurchaseChapterId = -1;
                showToast(getString(R.string.chapter_purchase_success));
                openReader(chapter.getId(), chapter.getNumber());
            }
        });
        viewModel.getRelatedComics().observe(getViewLifecycleOwner(), comics -> {
            relatedComicAdapter.submitList(comics);
            boolean hasRelated = comics != null && !comics.isEmpty();
            binding.tvComicDetailRelatedLabel.setVisibility(hasRelated ? View.VISIBLE : View.GONE);
            binding.rcvComicDetailRelated.setVisibility(hasRelated ? View.VISIBLE : View.GONE);
        });
        viewModel.getFollowed().observe(getViewLifecycleOwner(), isFollowed ->
                binding.btnComicDetailFollow.setText(getString(Boolean.TRUE.equals(isFollowed)
                        ? R.string.comic_unfollow
                        : R.string.comic_follow)));
        viewModel.getFollowLoading().observe(getViewLifecycleOwner(), isLoading ->
                binding.btnComicDetailFollow.setEnabled(isLoading == null || !isLoading));
        viewModel.getFollowSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });

        /* Translation observers */
        viewModel.getTranslating().observe(getViewLifecycleOwner(), isTranslating -> {
            boolean loading = Boolean.TRUE.equals(isTranslating);
            binding.prgComicDetailTranslate.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnComicDetailTranslate.setVisibility(loading ? View.GONE : View.VISIBLE);
        });
        viewModel.getShowingTranslation().observe(getViewLifecycleOwner(), showTranslation -> {
            if (Boolean.TRUE.equals(showTranslation)) {
                String tTitle = viewModel.getTranslatedTitle().getValue();
                String tSynopsis = viewModel.getTranslatedSynopsis().getValue();
                if (tTitle != null) binding.tvComicDetailTitle.setText(tTitle);
                if (tSynopsis != null) binding.tvComicDetailSynopsis.setText(tSynopsis);
                binding.tvComicDetailSynopsisLabel.setText(R.string.translate_show_original);
            } else if (currentComic != null) {
                binding.tvComicDetailTitle.setText(currentComic.getTitle());
                binding.tvComicDetailSynopsis.setText(currentComic.getSynopsis());
                binding.tvComicDetailSynopsisLabel.setText(R.string.comic_synopsis);
            }
        });

        /* Rating observers */
        viewModel.getRateMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            if (message.equals("SUCCESS")) {
                showToast(getString(R.string.comic_rate_success));
            } else if (message.startsWith("ERROR:")) {
                showToast(message.substring(6));
            }
        });
        viewModel.getRateLoading().observe(getViewLifecycleOwner(), isLoading ->
                binding.btnComicDetailRate.setEnabled(isLoading == null || !isLoading));
    }

    private void showRatingDialog() {
        if (sessionManager == null || !sessionManager.hasToken()) {
            showToast(getString(R.string.comic_rate_login_required));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.comic_rate_title));

        // Create layout programmatically: label + RatingBar
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        final TextView tvScore = new TextView(requireContext());
        tvScore.setTextSize(14);
        tvScore.setText(getString(R.string.comic_your_rating, 3));
        tvScore.setGravity(Gravity.CENTER_HORIZONTAL);

        final RatingBar ratingBar = new AppCompatRatingBar(requireContext(), null, android.R.attr.ratingBarStyle);
        android.widget.LinearLayout.LayoutParams ratingParams =
                new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        ratingParams.gravity = Gravity.CENTER_HORIZONTAL;
        ratingBar.setLayoutParams(ratingParams);
        ratingBar.setNumStars(5);
        ratingBar.setMax(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(3f);
        ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
            int score = Math.max(1, Math.round(rating));
            tvScore.setText(getString(R.string.comic_your_rating, score));
        });

        layout.addView(ratingBar);
        layout.addView(tvScore);
        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.comic_rate_submit), (dialog, which) -> {
            int score = Math.max(1, Math.round(ratingBar.getRating()));
            viewModel.rateComic(comicId, score);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private String formatViewCount(int viewCount) {
        if (viewCount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", viewCount / 1_000_000.0);
        } else if (viewCount >= 1_000) {
            return String.format(Locale.US, "%.1fK", viewCount / 1_000.0);
        } else {
            return String.valueOf(viewCount);
        }
    }

    private void toggleFollow() {

        if (sessionManager == null || !sessionManager.hasToken()) {
            showToast(getString(R.string.comic_follow_login_required));
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.loginFragment);
            }
            return;
        }
        viewModel.toggleFollow(comicId);
    }

    private void shareComicDeepLink() {
        if (currentComic == null || requireContext() == null) {
            return;
        }
        String url = com.group09.ComicReader.data.remote.ApiClient.toAbsolutePublicUrl(
                "/share/comic/" + currentComic.getId());
        String shareText = url;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_chooser_title));
        startActivity(shareIntent);
    }

    private void openReaderForChapter(Chapter chapter) {
        if (chapter == null) {
            return;
        }
        if (!chapter.isUnlocked()) {
            showPurchaseChapterDialog(chapter);
            return;
        }
        openReader(chapter.getId(), chapter.getNumber());
    }

    private void showPurchaseChapterDialog(@NonNull Chapter chapter) {
        if (sessionManager == null || !sessionManager.hasToken()) {
            showToast(getString(R.string.chapter_purchase_login_required));
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.loginFragment);
            }
            return;
        }

        String message;
        if (chapter.getPrice() > 0) {
            message = getString(R.string.chapter_purchase_confirm_with_price, chapter.getPrice());
        } else {
            message = getString(R.string.chapter_purchase_confirm);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.chapter_purchase_title))
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.chapter_purchase_top_up, (dialog, which) -> {
                    if (getView() != null) {
                        Navigation.findNavController(getView()).navigate(R.id.walletFragment);
                    }
                })
                .setPositiveButton(R.string.chapter_purchase_buy_now, (dialog, which) -> {
                    pendingPurchaseChapterId = chapter.getId();
                    viewModel.purchaseChapter(comicId, chapter);
                })
                .show();
    }

    private void openFirstAvailableChapter() {
        Chapter resumedChapter = findResumableChapter();
        if (resumedChapter != null) {
            openReader(resumedChapter.getId(), resumedChapter.getNumber());
            return;
        }
        for (Chapter chapter : currentChapters) {
            if (chapter != null && chapter.isUnlocked()) {
                openReader(chapter.getId(), chapter.getNumber());
                return;
            }
        }
        showToast(getString(com.group09.ComicReader.R.string.comic_no_unlocked_chapter));
    }

    private Chapter findResumableChapter() {
        if (readerProgressStore == null || currentComic == null || currentChapters == null || currentChapters.isEmpty()) {
            return null;
        }
        ReaderProgressStore.ReaderProgress progress = readerProgressStore.getProgress(currentComic.getId());
        if (progress == null) {
            return null;
        }
        for (Chapter chapter : currentChapters) {
            if (chapter == null) {
                continue;
            }
            if (chapter.getId() == progress.getChapterId() && chapter.isUnlocked()) {
                return chapter;
            }
        }
        return null;
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
