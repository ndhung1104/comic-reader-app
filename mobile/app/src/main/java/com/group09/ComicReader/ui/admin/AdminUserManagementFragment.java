package com.group09.ComicReader.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.databinding.FragmentAdminUserManagementBinding;
import com.group09.ComicReader.model.AdminUserResponse;
import com.group09.ComicReader.model.PageResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminUserManagementFragment extends BaseFragment {

    private FragmentAdminUserManagementBinding binding;
    private AdminRepository adminRepository;
    private AdminUserAdapter adapter;
    
    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private String currentSearch = "";
    private final List<AdminUserResponse> userList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserManagementBinding.inflate(inflater, container, false);
        adminRepository = new AdminRepository(new ApiClient(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadUsers(true);
    }

    private void setupToolbar() {
        binding.toolbarUserManagement.setNavigationOnClickListener(v -> 
                androidx.navigation.Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new AdminUserAdapter(new AdminUserAdapter.OnUserActionListener() {
            @Override
            public void onBan(AdminUserResponse user) {
                handleBanUnban(user, true);
            }

            @Override
            public void onUnban(AdminUserResponse user) {
                handleBanUnban(user, false);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rcvUsers.setLayoutManager(layoutManager);
        binding.rcvUsers.setAdapter(adapter);

        binding.rcvUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // check for scroll down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    // Threshold: Load more when there are 5 or fewer items left to scroll
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + pastVisibleItems) >= (totalItemCount - 5)) {
                            loadUsers(false);
                        }
                    }
                }
            }
        });
    }

    private void setupSearch() {
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            private java.util.Timer timer = new java.util.Timer();
            private final long DELAY = 500; // milliseconds

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (timer != null) timer.cancel();
            }

            @Override
            public void afterTextChanged(Editable s) {
                timer = new java.util.Timer();
                timer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            currentSearch = s.toString();
                            loadUsers(true);
                        });
                    }
                }, DELAY);
            }
        });
    }

    private void loadUsers(boolean refresh) {
        if (refresh) {
            currentPage = 0;
            isLastPage = false;
            userList.clear();
            binding.prgLoading.setVisibility(View.VISIBLE);
        }

        if (!refresh) {
            adapter.setLoaderVisible(true);
        }
        isLoading = true;
        adminRepository.getUsers(currentSearch, currentPage, 20, new AdminRepository.UserListCallback() {
            @Override
            public void onSuccess(@NonNull PageResponse<AdminUserResponse> page) {
                if (isDetached() || binding == null) {
                    isLoading = false;
                    return;
                }
                
                binding.prgLoading.setVisibility(View.GONE);
                adapter.setLoaderVisible(false);
                
                List<AdminUserResponse> newUsers = page.getContent();
                if (newUsers != null) {
                    userList.addAll(newUsers);
                    adapter.submitList(new ArrayList<>(userList));
                    
                    isLastPage = page.getNumber() + 1 >= page.getTotalPages();
                    currentPage++;
                }
                
                binding.tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                isLoading = false;
            }

            @Override
            public void onError(@NonNull String message) {
                if (isDetached() || binding == null) {
                    isLoading = false;
                    return;
                }
                binding.prgLoading.setVisibility(View.GONE);
                adapter.setLoaderVisible(false);
                showToast(message);
                isLoading = false;
            }
        });
    }

    private void handleBanUnban(AdminUserResponse user, boolean toBan) {
        AdminRepository.SimpleCallback callback = new AdminRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                showToast(message);
                // Simple way to refresh: just update the boolean in the local list
                user.setEnabled(!toBan);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(@NonNull String message) {
                showToast(message);
            }
        };

        if (toBan) {
            adminRepository.banUser(user.getId(), callback);
        } else {
            adminRepository.unbanUser(user.getId(), callback);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
