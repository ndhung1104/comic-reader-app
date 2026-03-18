package com.group09.ComicReader.ui.comment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.CommentAdapter;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentSheetBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(CommentSheetViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new CommentAdapter();
        binding.rcvCommentSheetComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvCommentSheetComments.setAdapter(adapter);

        binding.btnCommentSheetClose.setOnClickListener(v -> dismiss());
        binding.tilCommentSheetInput.setEndIconOnClickListener(v -> submitComment());

        int comicId = getArguments() == null ? 1 : getArguments().getInt(ARG_COMIC_ID, 1);
        viewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            adapter.submitList(comments);
            binding.rcvCommentSheetComments.scrollToPosition(0);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.loadComments(comicId);
    }

    private void submitComment() {
        String text = binding.edtCommentSheetInput.getText() == null ? "" : binding.edtCommentSheetInput.getText().toString();
        viewModel.addComment(text);
        binding.edtCommentSheetInput.setText("");
        android.widget.Toast.makeText(requireContext(), R.string.comment_posted, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
