package com.group09.ComicReader.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemModerationImportBinding;
import com.group09.ComicReader.model.ImportJobResponse;

import java.util.ArrayList;
import java.util.List;

public class ModerationImportsAdapter extends RecyclerView.Adapter<ModerationImportsAdapter.ViewHolder> {

    public interface OnModerationListener {
        void onApprove(ImportJobResponse job);
        void onReject(ImportJobResponse job);
    }

    private final List<ImportJobResponse> items = new ArrayList<>();
    private final OnModerationListener listener;

    public ModerationImportsAdapter(OnModerationListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ImportJobResponse> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemModerationImportBinding binding = ItemModerationImportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImportJobResponse job = items.get(position);
        holder.binding.tvSource.setText("Source: " + job.getSource());
        holder.binding.tvUrl.setText("URL: " + job.getSourceUrl());
        holder.binding.tvTime.setText("Created: " + job.getCreatedAt());

        holder.binding.btnApprove.setOnClickListener(v -> listener.onApprove(job));
        holder.binding.btnReject.setOnClickListener(v -> listener.onReject(job));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemModerationImportBinding binding;
        ViewHolder(ItemModerationImportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
