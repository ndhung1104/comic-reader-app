package com.group09.ComicReader.ui.ranking;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.group09.ComicReader.adapter.RankingAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentRankingBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.RankingViewModel;

import java.util.List;

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
        adapter.setStartRank(4);
        binding.rcvRankingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rcvRankingList.setAdapter(adapter);

        binding.imgRankingSearch.setOnClickListener(v -> Navigation.findNavController(v).navigate(com.group09.ComicReader.R.id.searchFragment));
        binding.cardRankingPodium1.setOnClickListener(v -> openComicDetail((Comic) v.getTag()));
        binding.cardRankingPodium2.setOnClickListener(v -> openComicDetail((Comic) v.getTag()));
        binding.cardRankingPodium3.setOnClickListener(v -> openComicDetail((Comic) v.getTag()));

        binding.btnRankingTabTop.setOnClickListener(v -> viewModel.loadTopRated());
        binding.btnRankingTabView.setOnClickListener(v -> viewModel.loadMostViewed());

        viewModel.getPodiumComics().observe(getViewLifecycleOwner(), this::bindPodium);
        viewModel.getRankedComics().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.getSelectedMode().observe(getViewLifecycleOwner(), mode ->
                updateTabState(mode == RankingViewModel.RankingMode.TOP_RATED));
        viewModel.loadTopRated();
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        RankingFragmentDirections.ActionRankingToComicDetail action =
                RankingFragmentDirections.actionRankingToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    private void bindPodium(@Nullable List<Comic> comics) {
        Comic first = comics != null && comics.size() > 0 ? comics.get(0) : null;
        Comic second = comics != null && comics.size() > 1 ? comics.get(1) : null;
        Comic third = comics != null && comics.size() > 2 ? comics.get(2) : null;

        bindPodiumSlot(first, binding.imgRankingPodiumCover1, binding.tvRankingPodiumTitle1, binding.cardRankingPodium1);
        bindPodiumSlot(second, binding.imgRankingPodiumCover2, binding.tvRankingPodiumTitle2, binding.cardRankingPodium2);
        bindPodiumSlot(third, binding.imgRankingPodiumCover3, binding.tvRankingPodiumTitle3, binding.cardRankingPodium3);
    }

    private void bindPodiumSlot(@Nullable Comic comic, @NonNull ImageView imageView, @NonNull TextView titleView, @NonNull View card) {
        card.setTag(comic);
        if (comic == null) {
            titleView.setText("");
            imageView.setImageDrawable(null);
            return;
        }
        titleView.setText(comic.getTitle());
        Glide.with(imageView).load(comic.getCoverUrl()).into(imageView);
    }

    private void updateTabState(boolean topRatedSelected) {
        updateTabButton(binding.btnRankingTabTop, topRatedSelected);
        updateTabButton(binding.btnRankingTabView, !topRatedSelected);
    }

    private void updateTabButton(@NonNull MaterialButton button, boolean selected) {
        int backgroundColor = ContextCompat.getColor(requireContext(),
                selected ? com.group09.ComicReader.R.color.surface_container_lowest : android.R.color.transparent);
        int textColor = ContextCompat.getColor(requireContext(),
                selected ? com.group09.ComicReader.R.color.primary : com.group09.ComicReader.R.color.on_surface_variant);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        button.setIconTint(ColorStateList.valueOf(textColor));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
