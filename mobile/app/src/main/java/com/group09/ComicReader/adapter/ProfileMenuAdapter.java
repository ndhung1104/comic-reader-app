package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemProfileMenuBinding;
import com.group09.ComicReader.model.ProfileMenuItem;

import java.util.ArrayList;
import java.util.List;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileMenuAdapter.ViewHolder> {

    public interface OnMenuClickListener {
        void onMenuClick(ProfileMenuItem item);
    }

    private final List<ProfileMenuItem> items = new ArrayList<>();
    private final OnMenuClickListener listener;

    public ProfileMenuAdapter(OnMenuClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProfileMenuItem> menuItems) {
        items.clear();
        if (menuItems != null) {
            items.addAll(menuItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileMenuBinding binding = ItemProfileMenuBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileMenuItem item = items.get(position);
        holder.binding.imgProfileMenuIcon.setImageResource(item.getIconResId());
        holder.binding.tvProfileMenuLabel.setText(item.getLabel());
        holder.binding.tvProfileMenuBadge.setVisibility(item.getBadge().isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.tvProfileMenuBadge.setText(item.getBadge());
        holder.binding.getRoot().setOnClickListener(v -> listener.onMenuClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProfileMenuBinding binding;

        ViewHolder(ItemProfileMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
