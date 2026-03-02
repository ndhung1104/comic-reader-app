package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.databinding.ItemWalletPackageBinding;
import com.group09.ComicReader.model.WalletPackage;

import java.util.ArrayList;
import java.util.List;

public class WalletPackageAdapter extends RecyclerView.Adapter<WalletPackageAdapter.ViewHolder> {

    public interface OnPackageClickListener {
        void onPackageClick(WalletPackage walletPackage);
    }

    private final List<WalletPackage> items = new ArrayList<>();
    private final OnPackageClickListener listener;

    public WalletPackageAdapter(OnPackageClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<WalletPackage> packages) {
        items.clear();
        if (packages != null) {
            items.addAll(packages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalletPackageBinding binding = ItemWalletPackageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletPackage walletPackage = items.get(position);
        holder.binding.tvWalletPackageCoins.setText(String.valueOf(walletPackage.getCoins()));
        holder.binding.tvWalletPackagePrice.setText(walletPackage.getPrice());
        boolean hasBonus = walletPackage.getBonus() != null && !walletPackage.getBonus().isEmpty();
        holder.binding.tvWalletPackageBonus.setVisibility(hasBonus ? View.VISIBLE : View.GONE);
        holder.binding.tvWalletPackageBonus.setText(walletPackage.getBonus());
        holder.binding.getRoot().setOnClickListener(v -> listener.onPackageClick(walletPackage));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWalletPackageBinding binding;

        ViewHolder(ItemWalletPackageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
