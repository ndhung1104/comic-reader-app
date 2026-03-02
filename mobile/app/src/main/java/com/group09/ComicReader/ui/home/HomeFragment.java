package com.group09.ComicReader.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.adapter.HomeDailyAdapter;
import com.group09.ComicReader.adapter.HomeRecommendedAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.databinding.FragmentHomeBinding;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.HomeViewModel;

import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private HomeDailyAdapter dailyAdapter;
    private HomeRecommendedAdapter recommendedAdapter;
    private Comic heroComic;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dailyAdapter = new HomeDailyAdapter(this::openComicDetail);
        recommendedAdapter = new HomeRecommendedAdapter(this::openComicDetail);

        binding.rcvHomeDailyList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rcvHomeDailyList.setAdapter(dailyAdapter);

        binding.rcvHomeRecommendedGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvHomeRecommendedGrid.setAdapter(recommendedAdapter);

        binding.btnHomeHeroRead.setOnClickListener(v -> {
            if (heroComic != null) {
                openComicDetail(heroComic);
            }
        });

        observeData();
        viewModel.loadData();
    }

    private void observeData() {
        viewModel.getTrendingComic().observe(getViewLifecycleOwner(), comics -> {
            if (comics == null || comics.isEmpty()) {
                return;
            }
            heroComic = comics.get(0);
            binding.tvHomeHeroTitle.setText(heroComic.getTitle());
            binding.tvHomeHeroMeta.setText(String.format(Locale.US, "%.1f - %s",
                    heroComic.getRating(), String.join(", ", heroComic.getGenres())));
            Glide.with(binding.imgHomeHeroCover)
                    .load(heroComic.getCoverUrl())
                    .into(binding.imgHomeHeroCover);
        });

        viewModel.getDailyUpdates().observe(getViewLifecycleOwner(), dailyAdapter::submitList);
        viewModel.getRecommended().observe(getViewLifecycleOwner(), recommendedAdapter::submitList);
    }

    private void openComicDetail(Comic comic) {
        if (comic == null || getView() == null) {
            return;
        }
        HomeFragmentDirections.ActionHomeToComicDetail action =
                HomeFragmentDirections.actionHomeToComicDetail(comic.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}