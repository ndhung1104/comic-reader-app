package com.group09.ComicReader.ui.creator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.CreatorRepository;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentCreatorStudioBinding;
import com.group09.ComicReader.model.ImportJobResponse;

public class CreatorStudioFragment extends BaseFragment {

    private FragmentCreatorStudioBinding binding;
    private CreatorRepository repository;
    private SessionManager sessionManager;
    private Long lastJobId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatorStudioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        sessionManager = new SessionManager(requireContext());
        repository = new CreatorRepository(new ApiClient(requireContext()));

        binding.btnEnqueue.setOnClickListener(v -> handleEnqueue());
        binding.btnCheckStatus.setOnClickListener(v -> {
            if (lastJobId == null) {
                showToast("No job to check");
                return;
            }
            fetchJobStatus(lastJobId);
        });
    }

    private void handleEnqueue() {
        if (!sessionManager.hasToken()) {
            showToast("Please log in as Creator to use this");
            return;
        }
        String url = binding.etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            showToast("Please enter an URL or slug");
            return;
        }

        String sourceType = binding.rgSourceType.getCheckedRadioButtonId() == binding.rbOtruyen.getId()
                ? "OTRUYEN"
                : "UNKNOWN";

        setLoading(true);

        repository.enqueueImport(url, sourceType, new CreatorRepository.EnqueueCallback() {
            @Override
            public void onSuccess(@NonNull ImportJobResponse response) {
                if (!isAdded())
                    return;
                setLoading(false);
                lastJobId = response.getId();
                binding.tvJobStatus.setVisibility(View.VISIBLE);
                binding.tvJobStatus.setText("Enqueued job #" + lastJobId + " (status=" + response.getStatus() + ")");
                binding.btnCheckStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded())
                    return;
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void fetchJobStatus(long jobId) {
        setLoading(true);
        repository.getJobStatus(jobId, new CreatorRepository.JobStatusCallback() {
            @Override
            public void onSuccess(@NonNull ImportJobResponse response) {
                if (!isAdded())
                    return;
                setLoading(false);
                binding.tvJobStatus.setVisibility(View.VISIBLE);
                String text = "Job #" + response.getId() + "\nstatus=" + response.getStatus();
                if (response.getResultComicId() != null)
                    text += "\ncomicId=" + response.getResultComicId();
                if (response.getErrorMessage() != null)
                    text += "\nerror=" + response.getErrorMessage();
                binding.tvJobStatus.setText(text);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded())
                    return;
                setLoading(false);
                showToast(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.btnEnqueue.setEnabled(!loading);
        binding.btnCheckStatus.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
