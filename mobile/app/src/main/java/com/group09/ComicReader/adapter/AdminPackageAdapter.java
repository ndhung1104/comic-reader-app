package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminPackageAdapter extends RecyclerView.Adapter<AdminPackageAdapter.ViewHolder> {
    
    public interface OnPackageToggleListener {
        void onToggle(long id, boolean isActive);
    }

    private final List<Map<String, Object>> items = new ArrayList<>();
    private final OnPackageToggleListener listener;

    public AdminPackageAdapter(OnPackageToggleListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Map<String, Object>> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_package, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        
        long id = item.containsKey("id") ? ((Number) item.get("id")).longValue() : 0;
        String name = item.containsKey("name") ? item.get("name").toString() : "";
        int coins = item.containsKey("coins") ? ((Number) item.get("coins")).intValue() : 0;
        String price = item.containsKey("priceLabel") ? item.get("priceLabel").toString() : "";
        boolean active = item.containsKey("active") && (Boolean) item.get("active");
        
        holder.tvName.setText(name);
        holder.tvCoins.setText(coins + " Coins");
        holder.tvPrice.setText("Price: " + price);
        
        if (active) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.positive_color));
            holder.btnToggle.setText(R.string.admin_packages_disable);
        } else {
            holder.tvStatus.setText("Disabled");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.danger_color));
            holder.btnToggle.setText(R.string.admin_packages_enable);
        }

        holder.btnToggle.setOnClickListener(v -> {
            if (listener != null) listener.onToggle(id, active);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvCoins, tvPrice;
        Button btnToggle;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvCoins = itemView.findViewById(R.id.tv_coins);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnToggle = itemView.findViewById(R.id.btn_toggle);
        }
    }
}
