package com.group09.ComicReader.ui.search;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.group09.ComicReader.R;

import java.util.ArrayList;
import java.util.List;

public class PageNumberAdapter extends RecyclerView.Adapter<PageNumberAdapter.VH> {

    public interface Listener {
        void onPageClicked(int page);
    }

    private final Context context;
    private final Listener listener;
    private final List<Integer> pages = new ArrayList<>();
    private int selected = 0;

    public PageNumberAdapter(Context ctx, Listener l) {
        this.context = ctx;
        this.listener = l;
    }

    public void setPages(List<Integer> list) {
        pages.clear();
        if (list != null)
            pages.addAll(list);
        notifyDataSetChanged();
    }

    public void setSelected(int sel) {
        this.selected = sel;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page_number, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        int pageIndex = pages.get(position);
        holder.btn.setText(String.valueOf(pageIndex + 1));

        if (position == selected) {
            holder.btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
            holder.btn.setTextColor(ContextCompat.getColor(context, R.color.on_primary));
            holder.btn.setStrokeWidth(0);
        } else {
            // outlined style
            holder.btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface)));
            holder.btn.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
            int strokePx = (int) (2 * context.getResources().getDisplayMetrics().density);
            holder.btn.setStrokeWidth(strokePx);
            holder.btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.outline_variant)));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onPageClicked(pageIndex);
        });
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialButton btn;

        VH(@NonNull View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.btn_page_number);
        }
    }
}
