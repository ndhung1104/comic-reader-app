package com.group09.ComicReader.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemModerationSummaryBinding;
import com.group09.ComicReader.model.AiSummaryResponse;

import java.util.ArrayList;
import java.util.List;

public class ModerationSummariesAdapter extends RecyclerView.Adapter<ModerationSummariesAdapter.ViewHolder> {

    public interface OnModerationListener {
        void onApprove(AiSummaryResponse summary);
        void onReject(AiSummaryResponse summary);
    }

    private final List<AiSummaryResponse> items = new ArrayList<>();
    private final OnModerationListener listener;

    public ModerationSummariesAdapter(OnModerationListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AiSummaryResponse> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemModerationSummaryBinding binding = ItemModerationSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiSummaryResponse summary = items.get(position);
        holder.binding.tvComicInfo.setText("Comic ID: " + summary.getComicId());
        holder.binding.tvContent.setText(summary.getContent());

        holder.binding.btnApprove.setOnClickListener(v -> listener.onApprove(summary));
        holder.binding.btnReject.setOnClickListener(v -> listener.onReject(summary));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemModerationSummaryBinding binding;
        ViewHolder(ItemModerationSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
