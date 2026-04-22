package com.group09.ComicReader.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminDashboardBinding;

public class AdminDashboardFragment extends BaseFragment {

    private FragmentAdminDashboardBinding binding;
    private AdminRepository adminRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        adminRepository = new AdminRepository(new ApiClient(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbarAdmin.setNavigationOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigateUp());

        binding.btnAdminUserManagement.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.action_admin_to_user_management));

        binding.btnAdminHideComment.setOnClickListener(v -> handleCommentAction(ActionType.HIDE));
        binding.btnAdminUnhideComment.setOnClickListener(v -> handleCommentAction(ActionType.UNHIDE));
        binding.btnAdminLockComment.setOnClickListener(v -> handleCommentAction(ActionType.LOCK));
        binding.btnAdminUnlockComment.setOnClickListener(v -> handleCommentAction(ActionType.UNLOCK));
        binding.btnAdminDeleteComment.setOnClickListener(v -> handleCommentAction(ActionType.DELETE));

        binding.btnAdminRevenue.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.action_admin_to_revenue));
        binding.btnAdminPackages.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.action_admin_to_packages));
        binding.btnAdminImport.setOnClickListener(v -> androidx.navigation.Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.action_admin_to_import));
    }


    private enum ActionType {
        HIDE, UNHIDE, LOCK, UNLOCK, DELETE
    }

    private void handleCommentAction(ActionType type) {
        String idStr = binding.edtAdminCommentId.getText() != null ? binding.edtAdminCommentId.getText().toString() : "";
        if (idStr.isEmpty()) {
            showToast("Please enter a Comment ID");
            return;
        }
        long commentId = Long.parseLong(idStr);
        binding.tvAdminStatus.setText("Processing comment action...");

        AdminRepository.SimpleCallback callback = new AdminRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                binding.tvAdminStatus.setText(message);
                showToast(message);
            }

            @Override
            public void onError(@NonNull String message) {
                binding.tvAdminStatus.setText("Error: " + message);
                showToast(message);
            }
        };

        switch (type) {
            case HIDE:
                adminRepository.hideComment(commentId, callback);
                break;
            case UNHIDE:
                adminRepository.unhideComment(commentId, callback);
                break;
            case LOCK:
                adminRepository.lockComment(commentId, callback);
                break;
            case UNLOCK:
                adminRepository.unlockComment(commentId, callback);
                break;
            case DELETE:
                adminRepository.deleteComment(commentId, callback);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
