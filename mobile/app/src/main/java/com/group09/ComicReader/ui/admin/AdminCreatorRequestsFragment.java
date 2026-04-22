package com.group09.ComicReader.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group09.ComicReader.R;
import com.group09.ComicReader.data.AdminRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.CreatorRequestPageResponse;
import com.group09.ComicReader.model.CreatorRequestResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminCreatorRequestsFragment extends Fragment implements AdminCreatorRequestsAdapter.ActionListener {

    private RecyclerView rv;
    private ProgressBar progressBar;
    private AdminCreatorRequestsAdapter adapter;
    private AdminRepository adminRepo;
    private int page = 0;
    private int size = 10;
    private boolean loading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_creator_requests, container, false);
        rv = v.findViewById(R.id.rv_requests);
        progressBar = v.findViewById(R.id.progress_bar);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminCreatorRequestsAdapter(new ArrayList<>(), this);
        rv.setAdapter(adapter);

        adminRepo = new AdminRepository(new ApiClient(requireContext()));

        v.findViewById(R.id.btn_load_more).setOnClickListener(view -> {
            if (!loading)
                loadPage(page + 1);
        });

        loadPage(0);

        return v;
    }

    private void loadPage(int p) {
        loading = true;
        progressBar.setVisibility(View.VISIBLE);
        adminRepo.getCreatorRequests(p, size, new AdminRepository.CreatorRequestsCallback() {
            @Override
            public void onSuccess(@NonNull CreatorRequestPageResponse pageResp) {
                progressBar.setVisibility(View.GONE);
                loading = false;
                page = pageResp.getPage();
                List<CreatorRequestResponse> items = pageResp.getItems();
                adapter.addItems(items);
            }

            @Override
            public void onError(@NonNull String message) {
                progressBar.setVisibility(View.GONE);
                loading = false;
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onApprove(@NonNull CreatorRequestResponse req) {
        adminRepo.approveCreatorRequest(req.getId(), "Approved from mobile",
                new AdminRepository.CreatorRequestActionCallback() {
                    @Override
                    public void onSuccess(@NonNull CreatorRequestResponse resp) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Approved", Toast.LENGTH_SHORT).show();
                            adapter.updateItem(resp);
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        requireActivity().runOnUiThread(
                                () -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                    }
                });
    }

    @Override
    public void onDeny(@NonNull CreatorRequestResponse req) {
        adminRepo.denyCreatorRequest(req.getId(), "Denied from mobile",
                new AdminRepository.CreatorRequestActionCallback() {
                    @Override
                    public void onSuccess(@NonNull CreatorRequestResponse resp) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Denied", Toast.LENGTH_SHORT).show();
                            adapter.updateItem(resp);
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        requireActivity().runOnUiThread(
                                () -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                    }
                });
    }
}
