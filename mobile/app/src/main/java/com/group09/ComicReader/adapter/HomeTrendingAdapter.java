package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemHomeTrendingBinding;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeTrendingAdapter extends RecyclerView.Adapter<HomeTrendingAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    private final List<Comic> items = new ArrayList<>();
    private final OnComicClickListener listener;

    public HomeTrendingAdapter(OnComicClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Comic> comics) {
        items.clear();
        if (comics != null) {
            items.addAll(comics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHomeTrendingBinding binding = ItemHomeTrendingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = items.get(position);
        String rankStr = String.format(Locale.US, "%02d", position + 1);
        holder.binding.tvHomeTrendingRank.setText(rankStr);
        holder.binding.tvHomeTrendingTitle.setText(comic.getTitle());
        holder.binding.tvHomeTrendingDesc.setText(comic.getSynopsis() != null ? comic.getSynopsis() : "No description available.");
        
        List<String> genres = comic.getGenres();
        if (!genres.isEmpty()) {
            holder.binding.tvHomeTrendingGenre1.setText(genres.get(0).toUpperCase());
            if (genres.size() > 1) {
                holder.binding.tvHomeTrendingGenre2.setText(genres.get(1).toUpperCase());
                holder.binding.tvHomeTrendingGenre2.setVisibility(android.view.View.VISIBLE);
            } else {
                holder.binding.tvHomeTrendingGenre2.setVisibility(android.view.View.GONE);
            }
        } else {
            holder.binding.tvHomeTrendingGenre1.setText("COMIC");
            holder.binding.tvHomeTrendingGenre2.setVisibility(android.view.View.GONE);
        }
        
        Glide.with(holder.binding.imgHomeTrendingCover)
                .load(comic.getCoverUrl())
                .into(holder.binding.imgHomeTrendingCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatViewCount(int viewCount) {
        if (viewCount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM views", viewCount / 1_000_000.0);
        }
        if (viewCount >= 1_000) {
            return String.format(Locale.US, "%.1fK views", viewCount / 1_000.0);
        }
        return viewCount + " views";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHomeTrendingBinding binding;

        ViewHolder(ItemHomeTrendingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
