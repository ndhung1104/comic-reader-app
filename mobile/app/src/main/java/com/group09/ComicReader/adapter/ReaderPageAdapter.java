package com.group09.ComicReader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.group09.ComicReader.databinding.ItemReaderPageBinding;
import com.group09.ComicReader.model.ReaderPage;

import java.util.List;
import java.util.Objects;

public class ReaderPageAdapter extends ListAdapter<ReaderPage, ReaderPageAdapter.PageViewHolder> {

    private static final float ZOOM_MIN_SCALE = 1.0f;
    private static final float ZOOM_MEDIUM_SCALE = 2.0f;
    private static final float ZOOM_MAX_SCALE = 5.0f;
    private static final float ZOOM_CHANGE_THRESHOLD = 0.01f;
    private static final float PAN_X_CHANGE_THRESHOLD = 0.01f;
    private static final int PRELOAD_AHEAD_DEFAULT = 4;
    private static final int PRELOAD_BEHIND_DEFAULT = 1;
    private static final int PRELOAD_AHEAD_ZOOMED = 8;
    private static final int PRELOAD_BEHIND_ZOOMED = 2;

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
    private boolean itemZoomEnabled = true;
    @Nullable
    private RecyclerView attachedRecyclerView;
    private int lastPreloadStart = -1;
    private int lastPreloadEnd = -1;
    private boolean lastPreloadZoomed;

    public ReaderPageAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setItemZoomEnabled(boolean enabled) {
        itemZoomEnabled = enabled;
        if (!itemZoomEnabled) {
            globalScale = ZOOM_MIN_SCALE;
            globalPanXNorm = 0.5f;
        }
        resetPreloadWindow();
    }

    @Override
    public void submitList(@Nullable List<ReaderPage> list) {
        resetPreloadWindow();
        super.submitList(list);
    }

    @Override
    public void submitList(@Nullable List<ReaderPage> list, @Nullable Runnable commitCallback) {
        resetPreloadWindow();
        super.submitList(list, commitCallback);
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
        holder.binding.imgReaderPage.setZoomGestureEnabled(itemZoomEnabled);
        if (!itemZoomEnabled) {
            holder.binding.imgReaderPage.setOnTransformChangeListener(null);
            holder.binding.imgReaderPage.setZoomTransform(ZOOM_MIN_SCALE, 0.5f);
        } else {
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
        }

        buildRequest(holder.binding.imgReaderPage, page)
                .into(holder.binding.imgReaderPage);
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
        resetPreloadWindow();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null;
        }
        resetPreloadWindow();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public void preloadAroundVisibleRange(int firstVisible, int lastVisible, boolean zoomed) {
        if (attachedRecyclerView == null || getItemCount() == 0) {
            return;
        }
        if (firstVisible == RecyclerView.NO_POSITION) {
            return;
        }
        int safeLastVisible = lastVisible == RecyclerView.NO_POSITION ? firstVisible : lastVisible;
        int preloadAhead = zoomed ? PRELOAD_AHEAD_ZOOMED : PRELOAD_AHEAD_DEFAULT;
        int preloadBehind = zoomed ? PRELOAD_BEHIND_ZOOMED : PRELOAD_BEHIND_DEFAULT;

        int targetStart = Math.max(0, firstVisible - preloadBehind);
        int targetEnd = Math.min(getItemCount() - 1, safeLastVisible + preloadAhead);
        if (targetStart > targetEnd) {
            return;
        }

        if (targetStart >= lastPreloadStart
                && targetEnd <= lastPreloadEnd
                && zoomed == lastPreloadZoomed) {
            return;
        }

        for (int index = targetStart; index <= targetEnd; index++) {
            preloadAt(index);
        }
        lastPreloadStart = targetStart;
        lastPreloadEnd = targetEnd;
        lastPreloadZoomed = zoomed;
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
        if (!itemZoomEnabled || attachedRecyclerView == null) {
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

    @NonNull
    private RequestBuilder<?> buildRequest(@NonNull View requestView, @NonNull ReaderPage page) {
        RequestBuilder<?> requestBuilder = Glide.with(requestView).load(page.getImageUrl());
        TargetSize targetSize = resolveTargetSize(requestView, page);
        if (targetSize.isValid()) {
            requestBuilder = requestBuilder.override(targetSize.width, targetSize.height);
        }
        return requestBuilder;
    }

    private void preloadAt(int position) {
        if (attachedRecyclerView == null || position < 0 || position >= getItemCount()) {
            return;
        }
        ReaderPage page = getItem(position);
        if (page == null || page.getImageUrl() == null || page.getImageUrl().trim().isEmpty()) {
            return;
        }
        buildRequest(attachedRecyclerView, page).preload();
    }

    @NonNull
    private TargetSize resolveTargetSize(@NonNull View referenceView, @NonNull ReaderPage page) {
        int targetWidth = resolveTargetWidthPx(referenceView);
        if (targetWidth <= 0) {
            return TargetSize.invalid();
        }
        if (page.getImageWidth() <= 0 || page.getImageHeight() <= 0) {
            if (referenceView instanceof ImageView) {
                ViewGroup.LayoutParams params = ((ImageView) referenceView).getLayoutParams();
                if (params != null && params.height > 0) {
                    return new TargetSize(targetWidth, params.height);
                }
            }
            return TargetSize.invalid();
        }
        float ratio = page.getImageHeight() / (float) page.getImageWidth();
        int targetHeight = Math.max(1, Math.round(targetWidth * ratio));
        return new TargetSize(targetWidth, targetHeight);
    }

    private int resolveTargetWidthPx(@NonNull View referenceView) {
        int width = referenceView.getWidth();
        if (width > 0) {
            return width;
        }
        if (attachedRecyclerView != null && attachedRecyclerView.getWidth() > 0) {
            return attachedRecyclerView.getWidth();
        }
        return referenceView.getResources().getDisplayMetrics().widthPixels;
    }

    private void resetPreloadWindow() {
        lastPreloadStart = -1;
        lastPreloadEnd = -1;
        lastPreloadZoomed = false;
    }

    private static final class TargetSize {
        final int width;
        final int height;

        TargetSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        static TargetSize invalid() {
            return new TargetSize(-1, -1);
        }

        boolean isValid() {
            return width > 0 && height > 0;
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
