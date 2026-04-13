package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemRankingBinding;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    private final List<Comic> items = new ArrayList<>();
    private final OnComicClickListener listener;
    private int startRank = 1;

    public RankingAdapter(OnComicClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Comic> comics) {
        items.clear();
        if (comics != null) {
            items.addAll(comics);
        }
        notifyDataSetChanged();
    }

    public void setStartRank(int startRank) {
        this.startRank = Math.max(1, startRank);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRankingBinding binding = ItemRankingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = items.get(position);
        holder.binding.tvRankingNumber.setText("#" + (startRank + position));
        holder.binding.tvRankingTitle.setText(comic.getTitle());
        holder.binding.tvRankingAuthor.setText(comic.getAuthor());
        holder.binding.tvRankingRating.setText(String.format(Locale.US, "%.1f", comic.getRating()));

        String genreText = comic.getGenres() != null ? String.join(", ", comic.getGenres()) : "";
        int views = comic.getViewCount();
        String viewText = views >= 1000
                ? String.format(Locale.US, "%.1fK views", views / 1000.0)
                : views + " views";
        holder.binding.tvRankingGenres.setText(genreText.isEmpty() ? viewText : genreText + " | " + viewText);

        int trendMode = position % 3;
        if (trendMode == 0) {
            holder.binding.imgRankingTrend.setImageResource(com.group09.ComicReader.R.drawable.ic_arrow_forward_24);
            holder.binding.imgRankingTrend.setRotation(-45f);
            holder.binding.imgRankingTrend.setColorFilter(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), com.group09.ComicReader.R.color.primary));
            holder.binding.imgRankingTrend.setContentDescription(
                    holder.binding.getRoot().getContext().getString(com.group09.ComicReader.R.string.ranking_trending_up));
        } else if (trendMode == 1) {
            holder.binding.imgRankingTrend.setImageResource(com.group09.ComicReader.R.drawable.ic_more_horiz_24);
            holder.binding.imgRankingTrend.setRotation(0f);
            holder.binding.imgRankingTrend.setColorFilter(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), com.group09.ComicReader.R.color.on_surface_variant));
            holder.binding.imgRankingTrend.setContentDescription(
                    holder.binding.getRoot().getContext().getString(com.group09.ComicReader.R.string.ranking_steady));
        } else {
            holder.binding.imgRankingTrend.setImageResource(com.group09.ComicReader.R.drawable.ic_arrow_forward_24);
            holder.binding.imgRankingTrend.setRotation(45f);
            holder.binding.imgRankingTrend.setColorFilter(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), com.group09.ComicReader.R.color.error));
            holder.binding.imgRankingTrend.setContentDescription(
                    holder.binding.getRoot().getContext().getString(com.group09.ComicReader.R.string.ranking_trending_down));
        }

        Glide.with(holder.binding.imgRankingCover)
                .load(comic.getCoverUrl())
                .into(holder.binding.imgRankingCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRankingBinding binding;

        ViewHolder(ItemRankingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
