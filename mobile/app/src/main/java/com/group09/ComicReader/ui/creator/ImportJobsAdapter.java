package com.group09.ComicReader.ui.creator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemCreatorImportJobBinding;
import com.group09.ComicReader.model.ImportJobResponse;

import java.util.ArrayList;
import java.util.List;

public class ImportJobsAdapter extends RecyclerView.Adapter<ImportJobsAdapter.ViewHolder> {

    private final List<ImportJobResponse> items = new ArrayList<>();

    public void submitList(List<ImportJobResponse> jobs) {
        items.clear();
        if (jobs != null) {
            items.addAll(jobs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCreatorImportJobBinding binding = ItemCreatorImportJobBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImportJobResponse job = items.get(position);
        holder.binding.tvSourceUrl.setText(job.getSourceUrl());
        holder.binding.tvStatus.setText(job.getStatus() != null ? job.getStatus() : "UNKNOWN");
        holder.binding.tvTime.setText(job.getCreatedAt() != null ? job.getCreatedAt() : "");

        if ("FAILED".equalsIgnoreCase(String.valueOf(job.getStatus()))) {
            holder.binding.tvError.setVisibility(View.VISIBLE);
            holder.binding.tvError.setText(job.getErrorMessage());
        } else {
            holder.binding.tvError.setVisibility(View.GONE);
        }

        if ("RUNNING".equalsIgnoreCase(String.valueOf(job.getStatus())) || "QUEUED".equalsIgnoreCase(String.valueOf(job.getStatus()))) {
            holder.binding.progressIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.binding.progressIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCreatorImportJobBinding binding;

        ViewHolder(ItemCreatorImportJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
