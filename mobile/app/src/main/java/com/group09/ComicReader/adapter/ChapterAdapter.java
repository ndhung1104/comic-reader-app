package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemComicChapterBinding;
import com.group09.ComicReader.model.Chapter;

import java.util.ArrayList;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    private final List<Chapter> items = new ArrayList<>();
    private final OnChapterClickListener listener;

    public ChapterAdapter(OnChapterClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Chapter> chapters) {
        items.clear();
        if (chapters != null) {
            items.addAll(chapters);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemComicChapterBinding binding = ItemComicChapterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chapter chapter = items.get(position);
        holder.binding.tvComicChapterTitle.setText(chapter.getTitle());
        holder.binding.tvComicChapterDate.setText(chapter.getReleaseDate());
        holder.binding.imgComicChapterLock.setVisibility(chapter.isPremium() ? View.VISIBLE : View.GONE);
        holder.binding.getRoot().setOnClickListener(v -> listener.onChapterClick(chapter));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemComicChapterBinding binding;

        ViewHolder(ItemComicChapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
