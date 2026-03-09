package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemReaderPageBinding;
import com.group09.ComicReader.model.ReaderPage;

import java.util.ArrayList;
import java.util.List;

public class ReaderPageAdapter extends RecyclerView.Adapter<ReaderPageAdapter.PageViewHolder> {

    private final List<ReaderPage> items = new ArrayList<>();

    public void submitList(List<ReaderPage> pages) {
        items.clear();
        if (pages != null) {
            items.addAll(pages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderPageBinding binding = ItemReaderPageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        ReaderPage page = items.get(position);
        Glide.with(holder.binding.imgReaderPage)
                .load(page.getImageUrl())
                .into(holder.binding.imgReaderPage);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ItemReaderPageBinding binding;

        PageViewHolder(ItemReaderPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
