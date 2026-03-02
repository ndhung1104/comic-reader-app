package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemLibraryComicBinding;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LibraryComicAdapter extends RecyclerView.Adapter<LibraryComicAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    private final List<Comic> items = new ArrayList<>();
    private final OnComicClickListener listener;

    public LibraryComicAdapter(OnComicClickListener listener) {
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
        ItemLibraryComicBinding binding = ItemLibraryComicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = items.get(position);
        int progress = comic.getProgress() == null ? 0 : comic.getProgress();
        int percentage = Math.round((progress * 100f) / comic.getTotalChapters());

        holder.binding.tvLibraryComicTitle.setText(comic.getTitle());
        holder.binding.tvLibraryComicAuthor.setText(comic.getAuthor());
        holder.binding.tvLibraryComicProgress.setText(String.format(Locale.US, "Chapter %d / %d (%d%%)", progress, comic.getTotalChapters(), percentage));
        holder.binding.pbLibraryComicProgress.setProgressCompat(percentage, true);
        Glide.with(holder.binding.imgLibraryComicCover)
                .load(comic.getCoverUrl())
                .into(holder.binding.imgLibraryComicCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
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
