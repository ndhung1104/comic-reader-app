package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemReaderPageBinding;
import com.group09.ComicReader.model.ReaderPage;

import java.util.Objects;

public class ReaderPageAdapter extends ListAdapter<ReaderPage, ReaderPageAdapter.PageViewHolder> {

    private static final DiffUtil.ItemCallback<ReaderPage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReaderPage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReaderPage oldItem, @NonNull ReaderPage newItem) {
                    return oldItem.getPageNumber() == newItem.getPageNumber();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ReaderPage oldItem, @NonNull ReaderPage newItem) {
                    return oldItem.getPageNumber() == newItem.getPageNumber()
                            && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
                }
            };

    public ReaderPageAdapter() {
        super(DIFF_CALLBACK);
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
        ReaderPage page = getItem(position);
        holder.binding.imgReaderPage.resetZoom();
        holder.binding.imgReaderPage.setMinimumScale(1.0f);
        holder.binding.imgReaderPage.setMediumScale(2.0f);
        holder.binding.imgReaderPage.setMaximumScale(5.0f);
        holder.binding.imgReaderPage.setAllowParentInterceptOnEdge(true);

        Glide.with(holder.binding.imgReaderPage)
                .load(page.getImageUrl())
                .into(holder.binding.imgReaderPage);

        preloadNextPages(holder, position);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        Glide.with(holder.binding.imgReaderPage).clear(holder.binding.imgReaderPage);
        super.onViewRecycled(holder);
    }

    private void preloadNextPages(@NonNull PageViewHolder holder, int position) {
        int preloadLimit = Math.min(getItemCount() - 1, position + 2);
        for (int index = position + 1; index <= preloadLimit; index++) {
            ReaderPage nextPage = getItem(index);
            if (nextPage == null || nextPage.getImageUrl() == null || nextPage.getImageUrl().trim().isEmpty()) {
                continue;
            }
            Glide.with(holder.binding.imgReaderPage)
                    .load(nextPage.getImageUrl())
                    .preload();
        }
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ItemReaderPageBinding binding;

        PageViewHolder(ItemReaderPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
