package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group09.ComicReader.databinding.ItemReaderPageBinding;
import com.group09.ComicReader.model.ReaderPage;

import java.util.Objects;

public class ReaderPageAdapter extends ListAdapter<ReaderPage, ReaderPageAdapter.PageViewHolder> {

    private static final float ZOOM_MIN_SCALE = 1.0f;
    private static final float ZOOM_MEDIUM_SCALE = 2.0f;
    private static final float ZOOM_MAX_SCALE = 5.0f;
    private static final float ZOOM_CHANGE_THRESHOLD = 0.01f;
    private static final float PAN_X_CHANGE_THRESHOLD = 0.01f;

    private static final DiffUtil.ItemCallback<ReaderPage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReaderPage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReaderPage oldItem, @NonNull ReaderPage newItem) {
                    return oldItem.getPageNumber() == newItem.getPageNumber();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ReaderPage oldItem, @NonNull ReaderPage newItem) {
                    return oldItem.getPageNumber() == newItem.getPageNumber()
                            && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                            && oldItem.getImageWidth() == newItem.getImageWidth()
                            && oldItem.getImageHeight() == newItem.getImageHeight();
                }
            };

    private float globalScale = ZOOM_MIN_SCALE;
    private float globalPanXNorm = 0.5f;
    @Nullable
    private RecyclerView attachedRecyclerView;

    public ReaderPageAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderPageBinding binding = ItemReaderPageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        ReaderPage page = getItem(position);
        applyPresetAspectRatio(holder, page);
        holder.binding.imgReaderPage.setMinimumScale(ZOOM_MIN_SCALE);
        holder.binding.imgReaderPage.setMediumScale(ZOOM_MEDIUM_SCALE);
        holder.binding.imgReaderPage.setMaximumScale(ZOOM_MAX_SCALE);
        holder.binding.imgReaderPage.setAllowParentInterceptOnEdge(true);
        holder.binding.imgReaderPage.setZoomTransform(globalScale, globalPanXNorm);
        holder.binding.imgReaderPage.setOnTransformChangeListener((scale, panXNorm, fromUser) -> {
            if (!fromUser) {
                return;
            }
            float clampedScale = clampScale(scale);
            float clampedPanXNorm = clampPanXNorm(panXNorm);
            boolean scaleChanged = Math.abs(globalScale - clampedScale) >= ZOOM_CHANGE_THRESHOLD;
            boolean panChanged = Math.abs(globalPanXNorm - clampedPanXNorm) >= PAN_X_CHANGE_THRESHOLD;
            if (!scaleChanged && !panChanged) {
                return;
            }
            globalScale = clampedScale;
            globalPanXNorm = clampedPanXNorm;
            applyScaleToAttachedHolders();
        });

        Glide.with(holder.binding.imgReaderPage)
                .load(page.getImageUrl())
                .into(holder.binding.imgReaderPage);

        preloadNextPages(holder, position);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        holder.binding.imgReaderPage.setOnTransformChangeListener(null);
        Glide.with(holder.binding.imgReaderPage).clear(holder.binding.imgReaderPage);
        super.onViewRecycled(holder);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attachedRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null;
        }
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private float clampScale(float scale) {
        if (scale < ZOOM_MIN_SCALE) {
            return ZOOM_MIN_SCALE;
        }
        if (scale > ZOOM_MAX_SCALE) {
            return ZOOM_MAX_SCALE;
        }
        return scale;
    }

    private float clampPanXNorm(float panXNorm) {
        if (panXNorm < 0f) {
            return 0f;
        }
        if (panXNorm > 1f) {
            return 1f;
        }
        return panXNorm;
    }

    private void applyScaleToAttachedHolders() {
        if (attachedRecyclerView == null) {
            return;
        }
        for (int index = 0; index < attachedRecyclerView.getChildCount(); index++) {
            View child = attachedRecyclerView.getChildAt(index);
            RecyclerView.ViewHolder viewHolder = attachedRecyclerView.getChildViewHolder(child);
            if (!(viewHolder instanceof PageViewHolder)) {
                continue;
            }
            PageViewHolder holder = (PageViewHolder) viewHolder;
            holder.binding.imgReaderPage.setZoomTransform(globalScale, globalPanXNorm);
        }
    }

    private void applyPresetAspectRatio(@NonNull PageViewHolder holder, @NonNull ReaderPage page) {
        ViewGroup.LayoutParams layoutParams = holder.binding.imgReaderPage.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        if (page.getImageWidth() <= 0 || page.getImageHeight() <= 0) {
            holder.binding.imgReaderPage.setAdjustViewBounds(true);
            if (layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.binding.imgReaderPage.setLayoutParams(layoutParams);
            }
            return;
        }

        int itemWidth = holder.itemView.getWidth();
        if (itemWidth <= 0) {
            itemWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels;
        }
        float ratio = page.getImageHeight() / (float) page.getImageWidth();
        int targetHeight = Math.max(1, Math.round(itemWidth * ratio));

        holder.binding.imgReaderPage.setAdjustViewBounds(false);
        if (layoutParams.height != targetHeight) {
            layoutParams.height = targetHeight;
            holder.binding.imgReaderPage.setLayoutParams(layoutParams);
        }
    }

    private void preloadNextPages(@NonNull PageViewHolder holder, int position) {
        int preloadLimit = Math.min(getItemCount() - 1, position + 2);
        for (int index = position + 1; index <= preloadLimit; index++) {
            ReaderPage nextPage = getItem(index);
            if (nextPage == null || nextPage.getImageUrl() == null || nextPage.getImageUrl().trim().isEmpty()) {
                continue;
            }
            Glide.with(holder.binding.imgReaderPage)
                    .load(nextPage.getImageUrl())
                    .preload();
        }
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ItemReaderPageBinding binding;

        PageViewHolder(ItemReaderPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
