package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.group09.ComicReader.databinding.ItemReaderEntryAdBinding;

public class ReaderEntryAdAdapter extends RecyclerView.Adapter<ReaderEntryAdAdapter.ReaderEntryAdViewHolder> {

    public interface Listener {
        void onAdImpression();

        void onAdUnavailable();
    }

    private final Listener listener;
    private boolean visible;
    private boolean adLoadStarted;
    private boolean impressionNotified;
    private String adUnitId;

    public ReaderEntryAdAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void update(boolean visible, @NonNull String adUnitId) {
        boolean stateChanged = this.visible != visible || !adUnitId.equals(this.adUnitId);
        this.visible = visible;
        this.adUnitId = adUnitId;
        if (!visible) {
            adLoadStarted = false;
            impressionNotified = false;
        }
        if (stateChanged) {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ReaderEntryAdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderEntryAdBinding binding = ItemReaderEntryAdBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReaderEntryAdViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReaderEntryAdViewHolder holder, int position) {
        if (!visible || adUnitId == null || adUnitId.trim().isEmpty()) {
            return;
        }

        if (!adLoadStarted) {
            adLoadStarted = true;
            holder.binding.adViewReaderEntryBanner.setAdUnitId(adUnitId);
            holder.binding.adViewReaderEntryBanner.setAdListener(new AdListener() {
                @Override
                public void onAdImpression() {
                    if (impressionNotified) {
                        return;
                    }
                    impressionNotified = true;
                    listener.onAdImpression();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    adLoadStarted = false;
                    listener.onAdUnavailable();
                }
            });
            holder.binding.adViewReaderEntryBanner.loadAd(new AdRequest.Builder().build());
        }
    }

    @Override
    public void onViewRecycled(@NonNull ReaderEntryAdViewHolder holder) {
        holder.binding.adViewReaderEntryBanner.setAdListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return visible ? 1 : 0;
    }

    static class ReaderEntryAdViewHolder extends RecyclerView.ViewHolder {
        private final ItemReaderEntryAdBinding binding;

        ReaderEntryAdViewHolder(@NonNull ItemReaderEntryAdBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
