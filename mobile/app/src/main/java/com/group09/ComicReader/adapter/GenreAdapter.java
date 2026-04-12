package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemGenreBinding;
import com.group09.ComicReader.model.CategoryPreview;

import java.util.ArrayList;
import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.ViewHolder> {

    public interface OnGenreClickListener {
        void onGenreClick(CategoryPreview genre);
    }

    private final List<CategoryPreview> items = new ArrayList<>();
    private final OnGenreClickListener listener;

    public GenreAdapter(OnGenreClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGenreBinding binding = ItemGenreBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    public void submitList(List<CategoryPreview> genres) {
        items.clear();
        if (genres != null) {
            items.addAll(genres);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryPreview preview = items.get(position);
        holder.binding.tvGenreName.setText(preview.getDisplayName());
        Glide.with(holder.binding.imgGenreOverlay)
                .load(preview.getCoverUrl())
                .into(holder.binding.imgGenreOverlay);
        holder.binding.getRoot().setOnClickListener(v -> listener.onGenreClick(preview));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemGenreBinding binding;

        ViewHolder(ItemGenreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
