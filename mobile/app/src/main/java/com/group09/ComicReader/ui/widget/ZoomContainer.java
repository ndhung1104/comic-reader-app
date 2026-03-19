package com.group09.ComicReader.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ZoomContainer extends FrameLayout {

    public enum InteractionState {
        IDLE,
        SCROLLING,
        ZOOMING,
        PANNING_ZOOMED
    }

    private static final float EPSILON = 0.001f;
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 3.0f;

    private final ScaleGestureDetector scaleGestureDetector;

    private boolean zoomEnabled;
    private float scaleFactor = MIN_SCALE;
    private float translationX = 0f;
    private float translationY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private InteractionState interactionState = InteractionState.IDLE;

    public ZoomContainer(@NonNull Context context) {
        this(context, null);
    }

    public ZoomContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setZoomEnabled(boolean enabled) {
        zoomEnabled = enabled;
        if (!zoomEnabled) {
            resetZoom();
        }
    }

    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    public InteractionState getInteractionState() {
        return interactionState;
    }

    public void resetZoom() {
        scaleFactor = MIN_SCALE;
        translationX = 0f;
        translationY = 0f;
        applyTransform();
        interactionState = InteractionState.IDLE;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!zoomEnabled || getZoomTarget() == null) {
            return super.onInterceptTouchEvent(event);
        }

        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                if (isZoomed()) {
                    interactionState = InteractionState.PANNING_ZOOMED;
                    return true;
                }
                interactionState = InteractionState.SCROLLING;
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                stopRecyclerScroll();
                interactionState = InteractionState.ZOOMING;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (interactionState == InteractionState.ZOOMING || interactionState == InteractionState.PANNING_ZOOMED) {
                    return true;
                }
                if (isZoomed()) {
                    interactionState = InteractionState.PANNING_ZOOMED;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                interactionState = isZoomed() ? InteractionState.PANNING_ZOOMED : InteractionState.IDLE;
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!zoomEnabled || getZoomTarget() == null) {
            return super.onTouchEvent(event);
        }

        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                stopRecyclerScroll();
                interactionState = InteractionState.ZOOMING;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!scaleGestureDetector.isInProgress() && isZoomed()) {
                    float currentX = event.getX();
                    float currentY = event.getY();
                    float dx = currentX - lastTouchX;
                    float dy = currentY - lastTouchY;
                    translationX += dx;
                    translationY += dy;
                    clampTranslation();
                    applyTransform();
                    interactionState = InteractionState.PANNING_ZOOMED;
                    lastTouchX = currentX;
                    lastTouchY = currentY;
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() > 1) {
                    int nextPointerIndex = event.getActionIndex() == 0 ? 1 : 0;
                    lastTouchX = event.getX(nextPointerIndex);
                    lastTouchY = event.getY(nextPointerIndex);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isZoomed()) {
                    resetZoom();
                } else {
                    interactionState = InteractionState.PANNING_ZOOMED;
                }
                return true;
            default:
                return true;
        }
    }

    private void applyTransform() {
        View zoomTarget = getZoomTarget();
        if (zoomTarget == null) {
            return;
        }
        zoomTarget.setPivotX(0f);
        zoomTarget.setPivotY(0f);
        zoomTarget.setScaleX(scaleFactor);
        zoomTarget.setScaleY(scaleFactor);
        zoomTarget.setTranslationX(translationX);
        zoomTarget.setTranslationY(translationY);
    }

    private void clampTranslation() {
        View zoomTarget = getZoomTarget();
        if (zoomTarget == null) {
            return;
        }
        if (!isZoomed()) {
            translationX = 0f;
            translationY = 0f;
            return;
        }

        float viewWidth = zoomTarget.getWidth();
        float viewHeight = zoomTarget.getHeight();
        float scaledWidth = viewWidth * scaleFactor;
        float scaledHeight = viewHeight * scaleFactor;

        float minX = Math.min(0f, viewWidth - scaledWidth);
        float minY = Math.min(0f, viewHeight - scaledHeight);
        float maxX = 0f;
        float maxY = 0f;

        translationX = clamp(translationX, minX, maxX);
        translationY = clamp(translationY, minY, maxY);
    }

    private float clamp(float value, float minValue, float maxValue) {
        if (value < minValue) {
            return minValue;
        }
        if (value > maxValue) {
            return maxValue;
        }
        return value;
    }

    @Nullable
    private View getZoomTarget() {
        if (getChildCount() == 0) {
            return null;
        }
        return getChildAt(0);
    }

    private void stopRecyclerScroll() {
        View target = getZoomTarget();
        if (target instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) target;
            recyclerView.stopScroll();
        }
    }

    private boolean isZoomed() {
        return scaleFactor > (MIN_SCALE + EPSILON);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            stopRecyclerScroll();
            interactionState = InteractionState.ZOOMING;
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            View zoomTarget = getZoomTarget();
            if (zoomTarget == null) {
                return false;
            }

            float previousScale = scaleFactor;
            float nextScale = clamp(previousScale * detector.getScaleFactor(), MIN_SCALE, MAX_SCALE);
            float scaleRatio = nextScale / previousScale;

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            translationX = focusX - scaleRatio * (focusX - translationX);
            translationY = focusY - scaleRatio * (focusY - translationY);

            scaleFactor = nextScale;
            clampTranslation();
            applyTransform();
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            if (!isZoomed()) {
                resetZoom();
                return;
            }
            interactionState = InteractionState.PANNING_ZOOMED;
        }
    }
}
