package com.group09.ComicReader.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.R;
import com.group09.ComicReader.databinding.ItemGenreCardBinding;
import com.group09.ComicReader.model.Genre;
import com.group09.ComicReader.model.Genre.LayoutType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenreCardAdapter extends RecyclerView.Adapter<GenreCardAdapter.ViewHolder> {

    private static final String VIEW_ALL_NAME = "View All";
    private static final String WEBTOON_NAME = "Webtoon";

    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }

    private final List<Genre> items = new ArrayList<>();
    private final OnGenreClickListener listener;
    private int spanCount = 2;

    public GenreCardAdapter(OnGenreClickListener listener) {
        this.listener = listener;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = Math.max(1, spanCount);
    }

    public void submitList(List<Genre> genres) {
        items.clear();
        if (genres != null) {
            items.addAll(genres);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGenreCardBinding binding = ItemGenreCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Genre genre = items.get(position);

        configureCardHeight(holder, genre);
        bindGenreCard(holder, genre);

        holder.binding.cardGenre.setOnClickListener(v -> listener.onGenreClick(genre));
    }

    private void bindGenreCard(@NonNull ViewHolder holder, @NonNull Genre genre) {
        if (VIEW_ALL_NAME.equals(genre.getName())) {
            bindViewAllCard(holder, genre);
            return;
        }

        bindStandardCard(holder, genre);
    }

    private void bindStandardCard(@NonNull ViewHolder holder, @NonNull Genre genre) {
        Context context = holder.binding.getRoot().getContext();

        holder.binding.llGenreViewAll.setVisibility(View.GONE);
        holder.binding.llGenreBottomContent.setVisibility(View.VISIBLE);
        holder.binding.imgGenreCover.setVisibility(View.VISIBLE);
        holder.binding.viewGenreGradient.setVisibility(View.VISIBLE);
        holder.binding.cardGenre.setStrokeWidth(0);
        holder.binding.cardGenre.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_highest));

        String displayName = genre.isMedium() && WEBTOON_NAME.equals(genre.getName())
                ? context.getString(R.string.browse_webtoon_original_title)
                : genre.getName();
        holder.binding.tvGenreName.setText(displayName);

        float titleSize = context.getResources().getDimension(
                genre.isLarge() ? R.dimen.browse_genre_large_name_size : R.dimen.browse_genre_name_size);
        holder.binding.tvGenreName.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);

        if (genre.getDescription() != null && !genre.getDescription().isEmpty()) {
            holder.binding.tvGenreDesc.setText(genre.getDescription());
            holder.binding.tvGenreDesc.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvGenreDesc.setVisibility(View.GONE);
        }

        if (genre.getTitleCount() > 0) {
            holder.binding.tvGenreCount.setText(String.format(Locale.US, "%,d Titles", genre.getTitleCount()));
            holder.binding.tvGenreCount.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvGenreCount.setVisibility(View.GONE);
        }

        if (genre.getBadge() != null && !genre.getBadge().isEmpty()) {
            holder.binding.tvGenreBadge.setText(genre.getBadge());
            holder.binding.tvGenreBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvGenreBadge.setVisibility(View.GONE);
        }

        holder.binding.llGenreCta.setVisibility(genre.isLarge() ? View.VISIBLE : View.GONE);
        applyTextColorScheme(holder, genre);

        holder.binding.viewGenreGradient.setBackgroundResource(
                resolveGradientResource(genre.getName(), genre.getLayoutType()));

        if (genre.getImageUrl() != null && !genre.getImageUrl().isEmpty()) {
            Glide.with(holder.binding.imgGenreCover)
                    .load(genre.getImageUrl())
                    .centerCrop()
                    .into(holder.binding.imgGenreCover);
        } else {
            holder.binding.imgGenreCover.setImageDrawable(null);
        }
    }

    private void bindViewAllCard(@NonNull ViewHolder holder, @NonNull Genre genre) {
        Context context = holder.binding.getRoot().getContext();

        holder.binding.imgGenreCover.setVisibility(View.GONE);
        holder.binding.viewGenreGradient.setVisibility(View.GONE);
        holder.binding.tvGenreBadge.setVisibility(View.GONE);
        holder.binding.llGenreBottomContent.setVisibility(View.GONE);
        holder.binding.llGenreCta.setVisibility(View.GONE);
        holder.binding.llGenreViewAll.setVisibility(View.VISIBLE);

        holder.binding.cardGenre.setStrokeWidth(0);
        holder.binding.cardGenre.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_highest));

        holder.binding.tvGenreViewAllTitle.setText(R.string.browse_view_all_title);
        if (genre.getDescription() != null && !genre.getDescription().isEmpty()) {
            holder.binding.tvGenreViewAllDesc.setText(genre.getDescription());
        } else {
            holder.binding.tvGenreViewAllDesc.setText(R.string.browse_view_all_subtitle);
        }
    }

    private void applyTextColorScheme(@NonNull ViewHolder holder, @NonNull Genre genre) {
        Context context = holder.binding.getRoot().getContext();

        if (genre.isMedium()) {
            int titleColor = ContextCompat.getColor(context, R.color.on_surface);
            int secondaryColor = ContextCompat.getColor(context, R.color.on_surface_variant);

            holder.binding.tvGenreName.setTextColor(titleColor);
            holder.binding.tvGenreDesc.setTextColor(secondaryColor);
            holder.binding.tvGenreCount.setTextColor(secondaryColor);
            holder.binding.tvGenreCta.setTextColor(titleColor);
            holder.binding.imgGenreCtaIcon.setColorFilter(titleColor);
        } else {
            int primaryText = ContextCompat.getColor(context, R.color.white);

            holder.binding.tvGenreName.setTextColor(primaryText);
            holder.binding.tvGenreDesc.setTextColor(primaryText);
            holder.binding.tvGenreCount.setTextColor(primaryText);
            holder.binding.tvGenreCta.setTextColor(primaryText);
            holder.binding.imgGenreCtaIcon.setColorFilter(primaryText);
        }
    }

    private void configureCardHeight(@NonNull ViewHolder holder, @NonNull Genre genre) {
        Context context = holder.binding.getRoot().getContext();
        int smallHeight = calculateSmallCardHeight(context);
        int minSmallHeight = context.getResources().getDimensionPixelSize(R.dimen.browse_genre_small_min_height);
        int minMediumHeight = context.getResources().getDimensionPixelSize(R.dimen.browse_genre_medium_min_height);
        int minLargeHeight = context.getResources().getDimensionPixelSize(R.dimen.browse_genre_large_min_height);
        int gutter = context.getResources().getDimensionPixelSize(R.dimen.spacing_lg);

        int twoCellWidth = spanCount > 1 ? (smallHeight * 2) + gutter : smallHeight;

        int targetHeight;
        if (genre.getLayoutType() == LayoutType.LARGE) {
            targetHeight = Math.max((int) (twoCellWidth * 1.18f), minLargeHeight);
        } else if (genre.getLayoutType() == LayoutType.MEDIUM) {
            targetHeight = Math.max((int) (twoCellWidth * 0.78f), minMediumHeight);
        } else {
            targetHeight = Math.max(smallHeight, minSmallHeight);
        }

        ViewGroup.LayoutParams params = holder.binding.cardGenre.getLayoutParams();
        if (params != null) {
            params.height = targetHeight;
            holder.binding.cardGenre.setLayoutParams(params);
        }
    }

    private int calculateSmallCardHeight(@NonNull Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int contentHorizontalPadding = context.getResources().getDimensionPixelSize(R.dimen.spacing_xxl) * 2;
        int recyclerHorizontalPadding = context.getResources().getDimensionPixelSize(R.dimen.spacing_xs) * 2;
        int itemHorizontalMargin = context.getResources().getDimensionPixelSize(R.dimen.spacing_sm) * 2 * spanCount;

        int availableWidth = screenWidth - contentHorizontalPadding - recyclerHorizontalPadding - itemHorizontalMargin;
        if (availableWidth <= 0) {
            return context.getResources().getDimensionPixelSize(R.dimen.browse_genre_small_min_height);
        }

        return availableWidth / spanCount;
    }

    private int resolveGradientResource(String genreName, LayoutType layoutType) {
        if (layoutType == LayoutType.MEDIUM) {
            return R.drawable.bg_gradient_genre_webtoon;
        }

        String key = genreName == null ? "" : genreName.toLowerCase(Locale.US);
        switch (key) {
            case "romance":
                return R.drawable.bg_gradient_genre_romance;
            case "isekai":
                return R.drawable.bg_gradient_genre_isekai;
            case "mystery":
                return R.drawable.bg_gradient_genre_mystery;
            case "shounen":
                return R.drawable.bg_gradient_genre_shounen;
            case "comedy":
                return R.drawable.bg_gradient_genre_comedy;
            case "action":
            default:
                return R.drawable.bg_gradient_genre_action;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemGenreCardBinding binding;

        ViewHolder(ItemGenreCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
