package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.ItemCommentBinding;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface Listener {
        void onReplyClicked(@NonNull CommentItem comment);
    }

    private final Listener listener;
    private final List<CommentItem> items = new ArrayList<>();

    public CommentAdapter() {
        this.listener = null;
    }

    public CommentAdapter(Listener listener) {
        this.listener = listener;
    }

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
        ItemCommentBinding binding =
                ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentItem comment = items.get(position);
        int depth = Math.max(0, comment.getDepth());
        String username = comment.getUsername() == null ? "" : comment.getUsername();
        holder.binding.tvCommentUsername.setText(depth > 0 ? "Reply: " + username : username);
        holder.binding.tvCommentTimestamp.setText(comment.getTimeAgo());
        holder.binding.tvCommentText.setText(comment.getContent());
        holder.binding.tvCommentLikes.setVisibility(android.view.View.GONE);

        String avatarUrl = comment.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            Glide.with(holder.binding.imgCommentAvatar).clear(holder.binding.imgCommentAvatar);
            holder.binding.imgCommentAvatar.setImageResource(R.drawable.ic_avatar_placeholder_24);
        } else {
            Glide.with(holder.binding.imgCommentAvatar)
                    .load(ApiClient.toAbsoluteUrl(avatarUrl))
                    .placeholder(R.drawable.ic_avatar_placeholder_24)
                    .error(R.drawable.ic_avatar_placeholder_24)
                    .circleCrop()
                    .into(holder.binding.imgCommentAvatar);
        }

        holder.binding.tvCommentLocked.setVisibility(comment.isLocked()
                ? android.view.View.VISIBLE
                : android.view.View.GONE);

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

        int baseStart = holder.itemView.getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        int baseEnd = holder.itemView.getResources().getDimensionPixelSize(R.dimen.spacing_lg);
        int indentPerLevel = holder.itemView.getResources().getDimensionPixelSize(R.dimen.spacing_xxxl);
        int extraIndent = Math.min(depth, 4) * indentPerLevel;

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) holder.binding.clCommentCard.getLayoutParams();
        lp.leftMargin = baseStart + extraIndent;
        lp.rightMargin = baseEnd;
        holder.binding.clCommentCard.setLayoutParams(lp);

        boolean canReply = listener != null && !comment.isLocked();
        holder.binding.tvCommentReply.setVisibility(canReply ? android.view.View.VISIBLE : android.view.View.GONE);
        holder.binding.tvCommentReply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyClicked(comment);
            }
        });
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
