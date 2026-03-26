package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemLibraryComicBinding;
import com.group09.ComicReader.model.LibraryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LibraryComicAdapter extends RecyclerView.Adapter<LibraryComicAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(LibraryItem item);
    }

    private final List<LibraryItem> items = new ArrayList<>();
    private final OnComicClickListener listener;

    public LibraryComicAdapter(OnComicClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<LibraryItem> comics) {
        items.clear();
        if (comics != null) {
            items.addAll(comics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLibraryComicBinding binding = ItemLibraryComicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryItem item = items.get(position);
        int progress = item.getProgressChapter() == null ? 0 : item.getProgressChapter();
        int total = item.getTotalChapters();
        int percentage = total <= 0 ? 0 : Math.min(100, Math.round((progress * 100f) / total));

        holder.binding.tvLibraryComicTitle.setText(item.getTitle());
        holder.binding.tvLibraryComicAuthor.setText(item.getAuthor());
        if (total > 0 && progress > 0) {
            holder.binding.tvLibraryComicProgress.setText(String.format(Locale.US, "%s (%d/%d)", item.getProgressLabel(), progress, total));
        } else {
            holder.binding.tvLibraryComicProgress.setText(item.getProgressLabel());
        }
        holder.binding.pbLibraryComicProgress.setProgressCompat(percentage, true);
        Glide.with(holder.binding.imgLibraryComicCover)
                .load(item.getCoverUrl())
                .into(holder.binding.imgLibraryComicCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemLibraryComicBinding binding;

        ViewHolder(ItemLibraryComicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
