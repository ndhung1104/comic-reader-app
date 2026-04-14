package com.group09.ComicReader.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.adapter.GenreAdapter;
import com.group09.ComicReader.adapter.HomeDailyAdapter;
import com.group09.ComicReader.adapter.HomeRecommendedAdapter;
import com.group09.ComicReader.adapter.HomeTrendingAdapter;
import com.group09.ComicReader.base.BaseFragment;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.databinding.FragmentHomeBinding;
import com.group09.ComicReader.model.CategoryPreview;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.viewmodel.HomeViewModel;

import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private HomeDailyAdapter dailyAdapter;
    private HomeRecommendedAdapter recommendedAdapter;
    private HomeTrendingAdapter topTrendingAdapter;
    private GenreAdapter genreAdapter;
    private Comic heroComic;
    private SessionManager sessionManager;

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
        topTrendingAdapter = new HomeTrendingAdapter(this::openComicDetail);
        genreAdapter = new GenreAdapter(this::openGenre);
        sessionManager = new SessionManager(requireContext());

        binding.rcvHomeDailyList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rcvHomeDailyList.setAdapter(dailyAdapter);

        binding.rcvHomeTrendingList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvHomeTrendingList.setNestedScrollingEnabled(false);
        binding.rcvHomeTrendingList.setAdapter(topTrendingAdapter);

        binding.rcvHomeRecommendedList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rcvHomeRecommendedList.setAdapter(recommendedAdapter);

        binding.rcvHomeGenres.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rcvHomeGenres.setAdapter(genreAdapter);

        binding.btnHomeHeroRead.setOnClickListener(v -> {
            if (heroComic != null) {
                openComicDetail(heroComic);
            }
        });
        binding.btnHomeHeroDetails.setOnClickListener(v -> {
            if (heroComic != null) {
                openComicDetail(heroComic);
            }
        });
        binding.tvHomeDailySeeAll.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.searchFragment));

        binding.btnHomeSearch.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.searchFragment);
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
        viewModel.getTopTrendingComics().observe(getViewLifecycleOwner(), comics -> {
            boolean hasTrending = comics != null && !comics.isEmpty();
            binding.tvHomeTrendingTitle.setVisibility(hasTrending ? View.VISIBLE : View.GONE);
            binding.rcvHomeTrendingList.setVisibility(hasTrending ? View.VISIBLE : View.GONE);
            topTrendingAdapter.submitList(comics);
        });
        viewModel.getRecommended().observe(getViewLifecycleOwner(), comics -> {
            boolean canShowRecommended = sessionManager.hasToken() && comics != null && !comics.isEmpty();
            binding.tvHomeRecommendedTitle.setVisibility(canShowRecommended ? View.VISIBLE : View.GONE);
            binding.rcvHomeRecommendedList.setVisibility(canShowRecommended ? View.VISIBLE : View.GONE);
            recommendedAdapter.submitList(canShowRecommended ? comics : java.util.Collections.emptyList());
        });
        viewModel.getCuratedGenres().observe(getViewLifecycleOwner(), previews -> {
            boolean hasGenres = previews != null && !previews.isEmpty();
            binding.tvHomeGenresTitle.setVisibility(hasGenres ? View.VISIBLE : View.GONE);
            binding.rcvHomeGenres.setVisibility(hasGenres ? View.VISIBLE : View.GONE);
            genreAdapter.submitList(previews);
        });
    }

    private void openGenre(CategoryPreview preview) {
        if (preview == null || getView() == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("initialFilter", preview.getCategoryId());
        Navigation.findNavController(getView()).navigate(R.id.searchFragment, args);
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
        if (binding != null) {
            binding.rcvHomeDailyList.setAdapter(null);
            binding.rcvHomeTrendingList.setAdapter(null);
            binding.rcvHomeRecommendedList.setAdapter(null);
            binding.rcvHomeGenres.setAdapter(null);
        }
        dailyAdapter = null;
        recommendedAdapter = null;
        topTrendingAdapter = null;
        genreAdapter = null;
        heroComic = null;
        super.onDestroyView();
        binding = null;
    }
}
