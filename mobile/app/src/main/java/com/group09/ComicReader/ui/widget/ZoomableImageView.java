package com.group09.ComicReader.ui.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private final Matrix drawMatrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final int touchSlop;

    private float minScale = 1.0f;
    private float mediumScale = 2.0f;
    private float maxScale = 5.0f;
    private float normalizedScale = 1.0f;
    private boolean allowParentInterceptOnEdge = true;
    private boolean isDragging;
    private float lastX;
    private float lastY;

    public ZoomableImageView(@NonNull Context context) {
        this(context, null);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setScaleType(ScaleType.MATRIX);
    }

    public void setMinimumScale(float scale) {
        if (scale <= 0f) {
            return;
        }
        minScale = scale;
        if (mediumScale < minScale) {
            mediumScale = minScale;
        }
        if (maxScale < mediumScale) {
            maxScale = mediumScale;
        }
    }

    public void setMediumScale(float scale) {
        if (scale < minScale) {
            return;
        }
        mediumScale = scale;
        if (maxScale < mediumScale) {
            maxScale = mediumScale;
        }
    }

    public void setMaximumScale(float scale) {
        if (scale < mediumScale) {
            return;
        }
        maxScale = scale;
    }

    public void setAllowParentInterceptOnEdge(boolean allowParentInterceptOnEdge) {
        this.allowParentInterceptOnEdge = allowParentInterceptOnEdge;
    }

    public void resetZoom() {
        resetMatrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            resetMatrix();
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        resetMatrix();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) {
            return super.onTouchEvent(event);
        }

        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                lastX = event.getX();
                lastY = event.getY();
                requestParentIntercept(normalizedScale > minScale);
                break;
            case MotionEvent.ACTION_MOVE:
                handleDrag(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                requestParentIntercept(false);
                break;
            default:
                break;
        }

        return normalizedScale > minScale
                || scaleDetector.isInProgress()
                || event.getPointerCount() > 1
                || super.onTouchEvent(event);
    }

    private void handleDrag(@NonNull MotionEvent event) {
        if (event.getPointerCount() > 1 || scaleDetector.isInProgress()) {
            return;
        }
        if (normalizedScale <= minScale) {
            requestParentIntercept(false);
            return;
        }

        float currentX = event.getX();
        float currentY = event.getY();
        float dx = currentX - lastX;
        float dy = currentY - lastY;

        if (!isDragging) {
            isDragging = Math.hypot(dx, dy) >= touchSlop;
        }
        if (!isDragging) {
            return;
        }

        drawMatrix.postTranslate(dx, dy);
        fixTranslation();
        setImageMatrix(drawMatrix);

        if (allowParentInterceptOnEdge && isVerticalEdgeReached(dy)) {
            requestParentIntercept(false);
        } else {
            requestParentIntercept(true);
        }

        lastX = currentX;
        lastY = currentY;
    }

    private boolean isVerticalEdgeReached(float dy) {
        RectF rect = getTransformedRect();
        if (rect == null || getHeight() == 0) {
            return false;
        }
        boolean atTop = rect.top >= 0 && dy > 0;
        boolean atBottom = rect.bottom <= getHeight() && dy < 0;
        return atTop || atBottom;
    }

    private void resetMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            normalizedScale = 1.0f;
            drawMatrix.reset();
            setImageMatrix(drawMatrix);
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float drawableWidth = drawable.getIntrinsicWidth();
        float drawableHeight = drawable.getIntrinsicHeight();

        if (drawableWidth <= 0f || drawableHeight <= 0f) {
            normalizedScale = 1.0f;
            drawMatrix.reset();
            setImageMatrix(drawMatrix);
            return;
        }

        float baseScale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * baseScale) * 0.5f;
        float dy = (viewHeight - drawableHeight * baseScale) * 0.5f;

        drawMatrix.reset();
        drawMatrix.postScale(baseScale, baseScale);
        drawMatrix.postTranslate(dx, dy);
        normalizedScale = 1.0f;
        setImageMatrix(drawMatrix);
    }

    private void fixTranslation() {
        RectF rect = getTransformedRect();
        if (rect == null) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float deltaX = 0f;
        float deltaY = 0f;

        if (rect.width() <= viewWidth) {
            deltaX = (viewWidth - rect.width()) * 0.5f - rect.left;
        } else if (rect.left > 0f) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }

        if (rect.height() <= viewHeight) {
            deltaY = (viewHeight - rect.height()) * 0.5f - rect.top;
        } else if (rect.top > 0f) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        drawMatrix.postTranslate(deltaX, deltaY);
    }

    @Nullable
    private RectF getTransformedRect() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }

        RectF rect = new RectF(0f, 0f, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawMatrix.mapRect(rect);
        return rect;
    }

    private void requestParentIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            requestParentIntercept(true);
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float targetScale = normalizedScale * scaleFactor;

            if (targetScale > maxScale) {
                scaleFactor = maxScale / normalizedScale;
            } else if (targetScale < minScale) {
                scaleFactor = minScale / normalizedScale;
            }

            drawMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            normalizedScale *= scaleFactor;
            fixTranslation();
            setImageMatrix(drawMatrix);
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            if (normalizedScale <= minScale) {
                requestParentIntercept(false);
            }
        }
    }
}
