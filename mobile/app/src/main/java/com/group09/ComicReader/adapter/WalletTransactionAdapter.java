package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.ItemWalletTransactionBinding;
import com.group09.ComicReader.model.WalletTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private final List<WalletTransaction> items = new ArrayList<>();

    public void submitList(List<WalletTransaction> transactions) {
        items.clear();
        if (transactions != null) {
            items.addAll(transactions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalletTransactionBinding binding = ItemWalletTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction transaction = items.get(position);
        holder.binding.tvWalletTransactionType.setText(capitalize(transaction.getType()));
        holder.binding.tvWalletTransactionDate.setText(transaction.getDate());
        holder.binding.tvWalletTransactionAmount.setText(transaction.getAmount() > 0
                ? "+" + transaction.getAmount()
                : String.valueOf(transaction.getAmount()));
        int color = transaction.getAmount() > 0
                ? ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.positive_color)
                : ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.text_secondary);
        holder.binding.tvWalletTransactionAmount.setTextColor(color);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWalletTransactionBinding binding;

        ViewHolder(ItemWalletTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
