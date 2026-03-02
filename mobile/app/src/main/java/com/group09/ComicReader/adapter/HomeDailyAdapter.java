package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemHomeDailyBinding;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class HomeDailyAdapter extends RecyclerView.Adapter<HomeDailyAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    private final List<Comic> items = new ArrayList<>();
    private final OnComicClickListener listener;

    public HomeDailyAdapter(OnComicClickListener listener) {
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
        ItemHomeDailyBinding binding = ItemHomeDailyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = items.get(position);
        holder.binding.tvHomeDailyTitle.setText(comic.getTitle());
        holder.binding.tvHomeDailyRating.setText(String.valueOf(comic.getRating()));
        Glide.with(holder.binding.imgHomeDailyCover)
                .load(comic.getCoverUrl())
                .into(holder.binding.imgHomeDailyCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHomeDailyBinding binding;

        ViewHolder(ItemHomeDailyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
