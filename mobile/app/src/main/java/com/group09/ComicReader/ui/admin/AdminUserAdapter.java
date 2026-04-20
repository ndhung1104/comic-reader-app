package com.group09.ComicReader.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.ItemAdminUserBinding;
import com.group09.ComicReader.model.AdminUserResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends ListAdapter<AdminUserResponse, AdminUserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onBan(AdminUserResponse user);
        void onUnban(AdminUserResponse user);
    }

    private final OnUserActionListener listener;

    public AdminUserAdapter(OnUserActionListener listener) {
        super(new DiffUtil.ItemCallback<AdminUserResponse>() {
            @Override
            public boolean areItemsTheSame(@NonNull AdminUserResponse oldItem, @NonNull AdminUserResponse newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull AdminUserResponse oldItem, @NonNull AdminUserResponse newItem) {
                return oldItem.isEnabled() == newItem.isEnabled() &&
                        oldItem.getEmail().equals(newItem.getEmail()) &&
                        oldItem.getFullName().equals(newItem.getFullName());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminUserBinding binding;

        UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AdminUserResponse user) {
            binding.tvUserName.setText(user.getFullName());
            binding.tvUserEmail.setText(user.getEmail());
            
            if (user.isEnabled()) {
                binding.tvUserStatus.setText("Enabled");
                binding.tvUserStatus.setTextColor(itemView.getContext().getColor(R.color.positive_color));
                binding.btnBan.setVisibility(View.VISIBLE);
                binding.btnUnban.setVisibility(View.GONE);
            } else {
                binding.tvUserStatus.setText("Disabled");
                binding.tvUserStatus.setTextColor(itemView.getContext().getColor(R.color.danger_color));
                binding.btnBan.setVisibility(View.GONE);
                binding.btnUnban.setVisibility(View.VISIBLE);
            }

            binding.btnBan.setOnClickListener(v -> listener.onBan(user));
            binding.btnUnban.setOnClickListener(v -> listener.onUnban(user));
        }
    }
}
