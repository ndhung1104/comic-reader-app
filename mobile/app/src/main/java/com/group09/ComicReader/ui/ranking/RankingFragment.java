package com.group09.ComicReader.ui.ranking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group09.ComicReader.adapter.RankingAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentRankingBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.RankingViewModel;

public class RankingFragment extends BaseFragment {

    private FragmentRankingBinding binding;
    private RankingViewModel viewModel;
    private RankingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(RankingViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new RankingAdapter(this::openComicDetail);
        binding.rcvRankingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvRankingList.setAdapter(adapter);

        viewModel.getRankedComics().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.loadData();
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        RankingFragmentDirections.ActionRankingToComicDetail action =
                RankingFragmentDirections.actionRankingToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
