package com.group09.ComicReader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.ItemReaderCommentsFooterBinding;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class ReaderCommentsFooterAdapter extends RecyclerView.Adapter<ReaderCommentsFooterAdapter.ViewHolder> {

    public interface Listener {
        void onSeeMoreClicked();

        void onSendComment(@NonNull String text);
    }

    private final Listener listener;
    private List<CommentItem> comments = new ArrayList<>();
    private boolean hasMore = false;
    private boolean isLoggedIn = false;

    public ReaderCommentsFooterAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setComments(List<CommentItem> comments) {
        this.comments = comments == null ? new ArrayList<>() : comments;
        notifyItemChanged(0);
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
        notifyItemChanged(0);
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderCommentsFooterBinding binding = ItemReaderCommentsFooterBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(comments, hasMore, isLoggedIn);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReaderCommentsFooterBinding binding;
        private final CommentAdapter commentAdapter;
        private final Listener listener;

        @Nullable
        private Boolean isLoggedIn;

        ViewHolder(ItemReaderCommentsFooterBinding binding, Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.commentAdapter = new CommentAdapter();
            binding.rcvReaderComments.setAdapter(commentAdapter);
            binding.tvReaderCommentsSeeMore.setOnClickListener(v -> listener.onSeeMoreClicked());

            binding.tilReaderCommentsInput.setEndIconOnClickListener(v -> submitComment());
        }

        void bind(List<CommentItem> comments, boolean hasMore, boolean isLoggedIn) {
            this.isLoggedIn = isLoggedIn;

            commentAdapter.submitList(comments);

            boolean hasComments = comments != null && !comments.isEmpty();
            binding.rcvReaderComments.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            binding.tvReaderCommentsEmpty.setVisibility(hasComments ? View.GONE : View.VISIBLE);

            binding.tvReaderCommentsSeeMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);

            binding.edtReaderCommentsInput.setEnabled(isLoggedIn);
            binding.tilReaderCommentsInput.setHint(isLoggedIn
                    ? binding.getRoot().getContext().getString(R.string.comment_hint)
                    : binding.getRoot().getContext().getString(R.string.comment_login_required));
        }

        private void submitComment() {
            Context context = binding.getRoot().getContext();

            if (isLoggedIn == null || !isLoggedIn) {
                Toast.makeText(context, R.string.comment_login_required, Toast.LENGTH_SHORT).show();
                return;
            }

            String text = binding.edtReaderCommentsInput.getText() == null
                    ? "" : binding.edtReaderCommentsInput.getText().toString();
            if (text.trim().isEmpty()) {
                return;
            }

            listener.onSendComment(text.trim());
            binding.edtReaderCommentsInput.setText("");
        }
    }
}
