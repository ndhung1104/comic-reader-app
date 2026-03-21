package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.ItemCommentBinding;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final List<CommentItem> items = new ArrayList<>();

    public void submitList(List<CommentItem> comments) {
        items.clear();
        if (comments != null) {
            items.addAll(comments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentItem comment = items.get(position);
        holder.binding.tvCommentUsername.setText(comment.getUsername());
        holder.binding.tvCommentTimestamp.setText(comment.getTimeAgo());
        holder.binding.tvCommentText.setText(comment.getContent());
        holder.binding.tvCommentLikes.setVisibility(android.view.View.GONE);

        if (comment.isLocked()) {
            holder.binding.tvCommentLocked.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvCommentLocked.setVisibility(android.view.View.GONE);
        }

        if (comment.getChapterNumber() != null && comment.getChapterNumber() > 0) {
            holder.binding.tvCommentSource.setText(
                    holder.itemView.getContext().getString(R.string.comment_chapter, comment.getChapterNumber()));
            holder.binding.tvCommentSource.setVisibility(android.view.View.VISIBLE);
        } else if ("SOCIAL_SHARE".equals(comment.getSourceType())) {
            holder.binding.tvCommentSource.setText("Social");
            holder.binding.tvCommentSource.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvCommentSource.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCommentBinding binding;

        ViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
