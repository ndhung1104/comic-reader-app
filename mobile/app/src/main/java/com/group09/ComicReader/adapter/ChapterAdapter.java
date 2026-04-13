package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemComicChapterBinding;
import com.group09.ComicReader.model.Chapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    private final List<Chapter> items = new ArrayList<>();
    private final OnChapterClickListener listener;
    private final Set<Long> downloadedChapterIds = new HashSet<>();

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

    public void setDownloadedChapterIds(@NonNull Set<Long> chapterIds) {
        downloadedChapterIds.clear();
        downloadedChapterIds.addAll(chapterIds);
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
        String subtitle = chapter.getReleaseDate();
        if (downloadedChapterIds.contains((long) chapter.getId())) {
            subtitle = subtitle + " \u2022 " + holder.binding.getRoot().getContext()
                    .getString(com.group09.ComicReader.R.string.download_completed_suffix);
        }
        holder.binding.tvComicChapterDate.setText(subtitle);
        holder.binding.imgComicChapterLock.setVisibility(chapter.isUnlocked() ? View.GONE : View.VISIBLE);
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
