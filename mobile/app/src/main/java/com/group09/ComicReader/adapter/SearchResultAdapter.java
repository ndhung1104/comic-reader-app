package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemSearchResultBinding;
import com.group09.ComicReader.model.Comic;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnComicClickListener {
        void onComicClick(Comic comic);
    }

    private final List<Comic> items = new ArrayList<>();
    private final OnComicClickListener listener;

    public SearchResultAdapter(OnComicClickListener listener) {
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
        ItemSearchResultBinding binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = items.get(position);
        holder.binding.tvSearchResultTitle.setText(comic.getTitle());
        holder.binding.tvSearchResultAuthor.setText(comic.getAuthor());
        holder.binding.tvSearchResultRating.setText(String.valueOf(comic.getRating()));
        holder.binding.tvSearchResultGenre.setText(comic.getGenres().isEmpty() ? "" : comic.getGenres().get(0));
        holder.binding.tvSearchResultNew.setVisibility(comic.isNew() ? View.VISIBLE : View.GONE);
        Glide.with(holder.binding.imgSearchResultCover)
                .load(comic.getCoverUrl())
                .into(holder.binding.imgSearchResultCover);
        holder.binding.getRoot().setOnClickListener(v -> listener.onComicClick(comic));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchResultBinding binding;

        ViewHolder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
