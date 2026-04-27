package com.group09.ComicReader.ui.creator;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemCreatorComicBinding;
import com.group09.ComicReader.model.ComicResponse;

import java.util.ArrayList;
import java.util.List;

public class CreatorComicsAdapter extends RecyclerView.Adapter<CreatorComicsAdapter.ViewHolder> {

    public interface OnComicActionListener {
        void onComicClick(ComicResponse comic);

        void onComicDelete(ComicResponse comic);
    }

    private final List<ComicResponse> items = new ArrayList<>();
    private final OnComicActionListener listener;

    public CreatorComicsAdapter(OnComicActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ComicResponse> comics) {
        items.clear();
        if (comics != null) {
            items.addAll(comics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCreatorComicBinding binding = ItemCreatorComicBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComicResponse comic = items.get(position);
        holder.binding.tvTitle.setText(comic.getTitle());
        holder.binding.tvInfo.setText(String.format("Views: %d • ID: %d", comic.getViewCount(), comic.getId()));
        holder.binding.chipStatus.setText(comic.getStatus() != null ? comic.getStatus().toUpperCase() : "DRAFT");

        Glide.with(holder.binding.imgCover)
                .load(comic.getCoverUrl())
                .placeholder(android.R.color.darker_gray)
                .into(holder.binding.imgCover);

        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onComicDelete(comic));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCreatorComicBinding binding;

        ViewHolder(ItemCreatorComicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
