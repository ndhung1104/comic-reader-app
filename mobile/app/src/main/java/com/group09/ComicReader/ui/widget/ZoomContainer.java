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
    private static final float SCALE_DEAD_ZONE = 0.015f;
    private static final float MULTI_TOUCH_DECISION_THRESHOLD_PX = 12f;
    private static final float MULTI_TOUCH_DECISION_RATIO = 1.15f;

    private final ScaleGestureDetector scaleGestureDetector;

    private enum MultiTouchMode {
        NONE,
        UNDECIDED,
        SCALE,
        PAN_2_FINGER
    }

    private boolean zoomEnabled;
    private float scaleFactor = MIN_SCALE;
    private float translationX = 0f;
    private float translationY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private float initialSpan = 0f;
    private float initialFocusX = 0f;
    private float initialFocusY = 0f;
    private float lastFocusX = 0f;
    private float lastFocusY = 0f;
    private MultiTouchMode multiTouchMode = MultiTouchMode.NONE;
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
        multiTouchMode = MultiTouchMode.NONE;
        interactionState = InteractionState.IDLE;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!zoomEnabled || getZoomTarget() == null) {
            return super.onInterceptTouchEvent(event);
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                multiTouchMode = MultiTouchMode.NONE;
                if (isZoomed()) {
                    interactionState = InteractionState.PANNING_ZOOMED;
                    return true;
                }
                interactionState = InteractionState.SCROLLING;
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                stopRecyclerScroll();
                beginMultiTouchSession(event);
                interactionState = InteractionState.PANNING_ZOOMED;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    return true;
                }
                if (interactionState == InteractionState.ZOOMING || interactionState == InteractionState.PANNING_ZOOMED || isZoomed()) {
                    interactionState = InteractionState.PANNING_ZOOMED;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_POINTER_UP:
                return event.getPointerCount() > 2 || isZoomed();
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                multiTouchMode = MultiTouchMode.NONE;
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

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                multiTouchMode = MultiTouchMode.NONE;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                stopRecyclerScroll();
                beginMultiTouchSession(event);
                interactionState = InteractionState.PANNING_ZOOMED;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    handleMultiTouchMove(event);
                    return true;
                }
                if (isZoomed()) {
                    handleSingleFingerPan(event);
                    interactionState = InteractionState.PANNING_ZOOMED;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_POINTER_UP:
                int remainingPointers = event.getPointerCount() - 1;
                if (remainingPointers >= 2) {
                    beginMultiTouchSession(event);
                } else if (remainingPointers == 1) {
                    int nextPointerIndex = event.getActionIndex() == 0 ? 1 : 0;
                    if (nextPointerIndex < event.getPointerCount()) {
                        lastTouchX = event.getX(nextPointerIndex);
                        lastTouchY = event.getY(nextPointerIndex);
                    }
                    multiTouchMode = MultiTouchMode.NONE;
                    interactionState = isZoomed() ? InteractionState.PANNING_ZOOMED : InteractionState.SCROLLING;
                } else {
                    multiTouchMode = MultiTouchMode.NONE;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                multiTouchMode = MultiTouchMode.NONE;
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

    private void beginMultiTouchSession(@NonNull MotionEvent event) {
        initialSpan = getSpan(event);
        initialFocusX = getFocusX(event);
        initialFocusY = getFocusY(event);
        lastFocusX = initialFocusX;
        lastFocusY = initialFocusY;
        multiTouchMode = MultiTouchMode.UNDECIDED;
    }

    private void handleSingleFingerPan(@NonNull MotionEvent event) {
        float currentX = event.getX();
        float currentY = event.getY();
        float dx = currentX - lastTouchX;
        float dy = currentY - lastTouchY;
        translationX += dx;
        translationY += dy;
        clampTranslation();
        applyTransform();
        lastTouchX = currentX;
        lastTouchY = currentY;
    }

    private void handleMultiTouchMove(@NonNull MotionEvent event) {
        float currentFocusX = getFocusX(event);
        float currentFocusY = getFocusY(event);
        float currentSpan = getSpan(event);

        if (multiTouchMode == MultiTouchMode.UNDECIDED) {
            float spanDelta = Math.abs(currentSpan - initialSpan);
            float focusDelta = distance(currentFocusX, currentFocusY, initialFocusX, initialFocusY);
            if (spanDelta >= MULTI_TOUCH_DECISION_THRESHOLD_PX
                    && spanDelta > (focusDelta * MULTI_TOUCH_DECISION_RATIO)) {
                multiTouchMode = MultiTouchMode.SCALE;
                interactionState = InteractionState.ZOOMING;
            } else if (focusDelta >= MULTI_TOUCH_DECISION_THRESHOLD_PX
                    && focusDelta > (spanDelta * MULTI_TOUCH_DECISION_RATIO)) {
                multiTouchMode = MultiTouchMode.PAN_2_FINGER;
                interactionState = InteractionState.PANNING_ZOOMED;
            } else {
                return;
            }
        }

        if (multiTouchMode == MultiTouchMode.SCALE) {
            scaleGestureDetector.onTouchEvent(event);
            lastFocusX = currentFocusX;
            lastFocusY = currentFocusY;
            return;
        }

        if (multiTouchMode == MultiTouchMode.PAN_2_FINGER) {
            if (isZoomed()) {
                float dx = currentFocusX - lastFocusX;
                float dy = currentFocusY - lastFocusY;
                translationX += dx;
                translationY += dy;
                clampTranslation();
                applyTransform();
                interactionState = InteractionState.PANNING_ZOOMED;
            }
            lastFocusX = currentFocusX;
            lastFocusY = currentFocusY;
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

    private float getFocusX(@NonNull MotionEvent event) {
        float total = 0f;
        int count = event.getPointerCount();
        for (int index = 0; index < count; index++) {
            total += event.getX(index);
        }
        return count == 0 ? 0f : total / count;
    }

    private float getFocusY(@NonNull MotionEvent event) {
        float total = 0f;
        int count = event.getPointerCount();
        for (int index = 0; index < count; index++) {
            total += event.getY(index);
        }
        return count == 0 ? 0f : total / count;
    }

    private float getSpan(@NonNull MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0f;
        }
        float x0 = event.getX(0);
        float y0 = event.getY(0);
        float x1 = event.getX(1);
        float y1 = event.getY(1);
        return distance(x0, y0, x1, y1);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.hypot(dx, dy);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            if (multiTouchMode != MultiTouchMode.SCALE) {
                return false;
            }
            stopRecyclerScroll();
            interactionState = InteractionState.ZOOMING;
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (multiTouchMode != MultiTouchMode.SCALE) {
                return false;
            }
            View zoomTarget = getZoomTarget();
            if (zoomTarget == null) {
                return false;
            }

            float detectorScale = detector.getScaleFactor();
            if (Math.abs(detectorScale - 1.0f) < SCALE_DEAD_ZONE) {
                return true;
            }

            float previousScale = scaleFactor;
            float nextScale = clamp(previousScale * detectorScale, MIN_SCALE, MAX_SCALE);
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
            multiTouchMode = MultiTouchMode.NONE;
            if (!isZoomed()) {
                resetZoom();
                return;
            }
            interactionState = InteractionState.PANNING_ZOOMED;
        }
    }
}
