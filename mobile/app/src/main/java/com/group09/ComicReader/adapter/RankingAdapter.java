package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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
        holder.binding.tvRankingNumber.setText("#" + (position + 1));
        holder.binding.tvRankingTitle.setText(comic.getTitle());
        holder.binding.tvRankingAuthor.setText(comic.getAuthor());
        holder.binding.tvRankingRating.setText(String.format(Locale.US, "%.1f", comic.getRating()));
        holder.binding.tvRankingGenres.setText(String.join(", ", comic.getGenres()));
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
