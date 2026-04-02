package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DailyRevenueAdapter extends RecyclerView.Adapter<DailyRevenueAdapter.ViewHolder> {
    private final List<Map<String, Object>> items = new ArrayList<>();

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_revenue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        
        String date = item.containsKey("date") ? item.get("date").toString() : "";
        
        Number topUp = (Number) item.get("topUp");
        Number purchase = (Number) item.get("purchase");
        Number total = (Number) item.get("total");
        
        long t = total != null ? total.longValue() : 0;
        long tu = topUp != null ? topUp.longValue() : 0;
        long p = purchase != null ? purchase.longValue() : 0;
        
        holder.tvDate.setText(date);
        holder.tvTotal.setText(String.format("Total: %d | TopUp: %d | Pur: %d", t, tu, p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvTotal;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTotal = itemView.findViewById(R.id.tv_total);
        }
    }
}
