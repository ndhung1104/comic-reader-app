package com.group09.ComicReader.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.model.CreatorRequestResponse;

import java.util.List;

public class AdminCreatorRequestsAdapter extends RecyclerView.Adapter<AdminCreatorRequestsAdapter.VH> {

    public interface ActionListener {
        void onApprove(@NonNull CreatorRequestResponse req);

        void onDeny(@NonNull CreatorRequestResponse req);
    }

    private final List<CreatorRequestResponse> items;
    private final ActionListener listener;

    public AdminCreatorRequestsAdapter(List<CreatorRequestResponse> items, ActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void addItems(List<CreatorRequestResponse> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void updateItem(CreatorRequestResponse updated) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == updated.getId()) {
                items.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_creator_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CreatorRequestResponse it = items.get(position);
        holder.tvUser.setText(it.getUserEmail());
        holder.tvCreated.setText(it.getCreatedAt());
        holder.tvMessage.setText(it.getMessage());
        holder.tvStatus.setText(it.getStatus());

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(it));
        holder.btnDeny.setOnClickListener(v -> listener.onDeny(it));

        if (it.getProcessedByEmail() != null) {
            holder.tvAudit.setVisibility(View.VISIBLE);
            holder.tvAudit.setText("By " + it.getProcessedByEmail() + " at " + it.getProcessedAt() + "\nMsg: "
                    + (it.getAdminMessage() == null ? "" : it.getAdminMessage()));
        } else {
            holder.tvAudit.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUser, tvCreated, tvMessage, tvStatus, tvAudit;
        com.google.android.material.button.MaterialButton btnApprove, btnDeny;

        VH(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tv_user);
            tvCreated = itemView.findViewById(R.id.tv_created);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvAudit = itemView.findViewById(R.id.tv_audit);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnDeny = itemView.findViewById(R.id.btn_deny);
        }
    }
}
