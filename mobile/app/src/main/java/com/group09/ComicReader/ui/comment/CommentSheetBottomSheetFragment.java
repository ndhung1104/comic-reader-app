package com.group09.ComicReader.ui.comment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.CommentAdapter;
import com.group09.ComicReader.common.error.ErrorParser;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentCommentSheetBinding;
import com.group09.ComicReader.viewmodel.CommentsViewModel;

public class CommentSheetBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_COMIC_ID = "comic_id";
    private static final String ARG_CHAPTER_ID = "chapter_id";

    public static CommentSheetBottomSheetFragment newInstance(int comicId) {
        return newInstance(comicId, null);
    }

    public static CommentSheetBottomSheetFragment newInstance(int comicId, Integer chapterId) {
        CommentSheetBottomSheetFragment fragment = new CommentSheetBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COMIC_ID, comicId);
        if (chapterId != null) {
            args.putInt(ARG_CHAPTER_ID, chapterId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentCommentSheetBinding binding;
    private CommentsViewModel viewModel;
    private CommentAdapter adapter;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentSheetBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        sessionManager = new SessionManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupInputArea();
        setupObservers();

        int comicId = getArguments() == null ? 1 : getArguments().getInt(ARG_COMIC_ID, 1);
        int chapterId = getArguments() == null ? 0 : getArguments().getInt(ARG_CHAPTER_ID, 0);
        viewModel.init(comicId, chapterId <= 0 ? null : chapterId);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        if (params != null) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.setLayoutParams(params);
        }

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(comment -> {
            if (!sessionManager.hasToken()) {
                Toast.makeText(requireContext(), R.string.comment_login_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.startReply(comment);
            binding.edtCommentSheetInput.requestFocus();
        });
        binding.rcvCommentSheetComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvCommentSheetComments.setAdapter(adapter);
        binding.btnCommentSheetClose.setOnClickListener(v -> dismiss());

        binding.tvCommentSheetSeeMore.setOnClickListener(v -> viewModel.loadMore());
    }

    private void setupInputArea() {
        binding.btnCommentSheetSend.setOnClickListener(v -> {
            if (!sessionManager.hasToken()) {
                Toast.makeText(requireContext(), R.string.comment_login_required, Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment();
        });
        binding.tvCommentSheetCancelReply.setOnClickListener(v -> viewModel.cancelReply());
        refreshInputLoginState();
    }

    private void setupObservers() {
        // Comments list
        viewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            adapter.submitList(comments);

            boolean hasComments = comments != null && !comments.isEmpty();
            binding.rcvCommentSheetComments.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            binding.tvCommentSheetEmpty.setVisibility(hasComments ? View.GONE : View.VISIBLE);

            int count = comments == null ? 0 : comments.size();
            binding.tvCommentSheetTitle.setText(R.string.comment_discussion_title);
            binding.tvCommentSheetSubtitle.setText(getString(R.string.comment_discussion_subtitle, count));
        });

        viewModel.getHasMore().observe(getViewLifecycleOwner(), more -> {
            boolean show = more != null && more;
            binding.tvCommentSheetSeeMore.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        // Loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.pbCommentSheetLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.rcvCommentSheetComments.setVisibility(View.GONE);
                binding.tvCommentSheetEmpty.setVisibility(View.GONE);
            }
        });

        // Post success
        viewModel.getPostSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(requireContext(), R.string.comment_posted, Toast.LENGTH_SHORT).show();
                binding.edtCommentSheetInput.setText("");
                binding.rcvCommentSheetComments.scrollToPosition(0);
            }
        });
        viewModel.getReplyingToLabel().observe(getViewLifecycleOwner(), label -> {
            boolean isReplying = label != null && !label.trim().isEmpty();
            binding.clCommentSheetReplying.setVisibility(isReplying ? View.VISIBLE : View.GONE);
            if (isReplying) {
                binding.tvCommentSheetReplyingTo.setText(label);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                if (ErrorParser.isTokenExpiredMessage(error)) {
                    sessionManager.clear();
                    binding.edtCommentSheetInput.setText("");
                    viewModel.cancelReply();
                    refreshInputLoginState();
                }
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshInputLoginState() {
        boolean isLoggedIn = sessionManager.hasToken();
        binding.edtCommentSheetInput.setEnabled(isLoggedIn);
        binding.tilCommentSheetInput.setHint(isLoggedIn
                ? getString(R.string.comment_hint)
                : getString(R.string.comment_login_required));
        binding.btnCommentSheetSend.setAlpha(isLoggedIn ? 1f : 0.5f);
    }

    private void submitComment() {
        String text = binding.edtCommentSheetInput.getText() == null
                ? "" : binding.edtCommentSheetInput.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }
        viewModel.addComment(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
