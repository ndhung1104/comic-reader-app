package com.group09.ComicReader.ui.comment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.CommentAdapter;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentCommentSheetBinding;
import com.group09.ComicReader.viewmodel.CommentSheetViewModel;

public class CommentSheetBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_COMIC_ID = "comic_id";

    public static CommentSheetBottomSheetFragment newInstance(int comicId) {
        CommentSheetBottomSheetFragment fragment = new CommentSheetBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COMIC_ID, comicId);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentCommentSheetBinding binding;
    private CommentSheetViewModel viewModel;
    private CommentAdapter adapter;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentSheetBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(CommentSheetViewModel.class);
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
        viewModel.loadComments(comicId);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter();
        binding.rcvCommentSheetComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvCommentSheetComments.setAdapter(adapter);
        binding.btnCommentSheetClose.setOnClickListener(v -> dismiss());
    }

    private void setupInputArea() {
        boolean isLoggedIn = sessionManager.hasToken();

        if (isLoggedIn) {
            binding.tilCommentSheetInput.setVisibility(View.VISIBLE);
            binding.tilCommentSheetInput.setEndIconOnClickListener(v -> submitComment());
        } else {
            binding.tilCommentSheetInput.setVisibility(View.VISIBLE);
            binding.edtCommentSheetInput.setEnabled(false);
            binding.edtCommentSheetInput.setHint(R.string.comment_login_required);
            binding.tilCommentSheetInput.setEndIconOnClickListener(v ->
                    Toast.makeText(requireContext(), R.string.comment_login_required, Toast.LENGTH_SHORT).show());
        }
    }

    private void setupObservers() {
        // Comments list
        viewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            adapter.submitList(comments);

            boolean hasComments = comments != null && !comments.isEmpty();
            binding.rcvCommentSheetComments.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            binding.tvCommentSheetEmpty.setVisibility(hasComments ? View.GONE : View.VISIBLE);

            // Update title with count
            int count = comments == null ? 0 : comments.size();
            binding.tvCommentSheetTitle.setText(getString(R.string.comment_count, count));

            if (hasComments) {
                binding.rcvCommentSheetComments.scrollToPosition(0);
            }
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
            }
        });

        // Error message
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitComment() {
        String text = binding.edtCommentSheetInput.getText() == null
                ? "" : binding.edtCommentSheetInput.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }
        viewModel.addComment(text);
        binding.edtCommentSheetInput.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
