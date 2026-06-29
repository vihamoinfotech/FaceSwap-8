package com.facechanger.faceswap.enhance.view.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facechanger.faceswap.enhance.R;

/**
 * Production-quality "Before / After" image comparison slider.
 * <p>
 * <b>Layer order (bottom → top):</b>
 * <ol>
 *   <li>BEFORE image — fully visible (bottom layer)</li>
 *   <li>AFTER image  — clipped from the left edge up to the slider position (top layer)</li>
 *   <li>Divider line + drag handle</li>
 *   <li>Labels ("Before" / "After")</li>
 * </ol>
 * <p>
 * <b>Behaviour:</b>
 * <ul>
 *   <li>Slider starts at 50% (center)</li>
 *   <li>Left of slider → BEFORE image visible</li>
 *   <li>Right of slider → AFTER image visible</li>
 *   <li>Drag right → reveal more AFTER</li>
 *   <li>Drag left → reveal more BEFORE</li>
 * </ul>
 * <p>
 * The AFTER image is clipped using {@link View#setClipBounds(Rect)} on the
 * {@link ImageView} itself. Because the clip rect always starts at x=0 and
 * the image uses {@code centerCrop}, the visible portion shifts correctly
 * without any squishing or blank areas.
 */
public class AppFaceBeforeAfterSliderView extends FrameLayout {

    private static final String TAG = "BeforeAfterSliderView";

    // ── Views ──
    private ImageView ivBefore;    // BOTTOM layer — always fully visible
    private ImageView ivAfter;     // TOP layer   — clipped from left
    private View sliderLine;
    private ImageView sliderHandle;
    private ProgressBar progressBar;
    private View tvBeforeLabel;
    private View tvAfterLabel;

    // ── State ──
    private float sliderPosition = 0.5f;   // 0 = fully BEFORE, 1 = fully AFTER
    private int viewWidth = 0;
    private int viewHeight = 0;
    private boolean beforeReady = false;
    private boolean afterReady = false;

    // ── Constructors ──

    public AppFaceBeforeAfterSliderView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AppFaceBeforeAfterSliderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppFaceBeforeAfterSliderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ── Initialisation ──

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.app_face_before_after_slider_view, this, true);
        setClickable(true);
        setFocusable(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ivBefore = findViewById(R.id.ivSliderBefore);
        ivAfter = findViewById(R.id.ivSliderAfter);
        sliderLine = findViewById(R.id.sliderLine);
        sliderHandle = findViewById(R.id.sliderHandle);
        progressBar = findViewById(R.id.sliderProgress);
        tvBeforeLabel = findViewById(R.id.tvSliderBeforeLabel);
        tvAfterLabel = findViewById(R.id.tvSliderAfterLabel);

        setOnTouchListener((v, event) -> {
            if (viewWidth <= 0) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    moveSliderTo(event.getX() / viewWidth);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    moveSliderTo(event.getX() / viewWidth);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return false;
            }
        });

        post(() -> {
            viewWidth = getWidth();
            viewHeight = getHeight();
            moveSliderTo(sliderPosition);
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            viewWidth = w;
            viewHeight = h;
            moveSliderTo(sliderPosition);
        }
    }

    // ── Public API ──

    /**
     * Loads before (original) and after (result) images using Glide.
     * <p>
     * Accepts any type Glide can handle: String URL, File, GlideUrl, etc.
     * Shows a progress indicator until both images have loaded (or failed),
     * then reveals the slider at the 50% mark.
     *
     * @param beforeModel The user's original image (left side / "Before")
     * @param afterModel  The API result image (right side / "After")
     */
    public void setImages(@Nullable Object beforeModel, @Nullable Object afterModel) {
        if (beforeModel == null || afterModel == null) {
            Log.w(TAG, "setImages: one or both models are null — hiding slider UI");
            showProgress(false);
            return;
        }

        // Reset state
        beforeReady = false;
        afterReady = false;
        showProgress(true);
        hideSliderUI();

        // Load BEFORE image (bottom layer)
        loadImage(beforeModel, ivBefore, true);

        // Load AFTER image (top layer, will be clipped)
        loadImage(afterModel, ivAfter, false);
    }

    /**
     * Sets bitmap images directly (for cases where bitmaps are already in memory).
     */
    public void setImages(@Nullable Bitmap beforeBitmap, @Nullable Bitmap afterBitmap) {
        if (beforeBitmap == null || afterBitmap == null) {
            Log.w(TAG, "setImages(Bitmap): one or both bitmaps are null");
            return;
        }
        try {
            ivBefore.setImageBitmap(beforeBitmap);
            ivAfter.setImageBitmap(afterBitmap);
            beforeReady = true;
            afterReady = true;
            checkBothReady();
        } catch (Exception e) {
            Log.e(TAG, "Error setting bitmaps", e);
        }
    }

    /**
     * Displays a single image as a static preview — no slider, no comparison.
     * Used when only the API result is available (no original/before image).
     *
     * @param model The image to display (Glide-compatible: URL, File, GlideUrl, etc.)
     */
    public void setSingleImage(@Nullable Object model) {
        if (model == null) {
            Log.w(TAG, "setSingleImage: model is null");
            return;
        }

        // Hide slider UI — no comparison
        hideSliderUI();

        // Hide the BEFORE layer entirely
        if (ivBefore != null) ivBefore.setVisibility(GONE);

        // Remove clip from AFTER so it fills the whole view
        if (ivAfter != null) ivAfter.setClipBounds(null);

        // Load the single image into the AFTER layer
        showProgress(true);
        try {
            Glide.with(getContext())
                    .load(model)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object m,
                                                    @NonNull Target<Drawable> t, boolean isFirst) {
                            Log.e(TAG, "Single image load failed", e);
                            showProgress(false);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object m,
                                                       @NonNull Target<Drawable> t, @NonNull DataSource ds,
                                                       boolean isFirst) {
                            showProgress(false);
                            return false;
                        }
                    })
                    .into(ivAfter);
        } catch (Exception e) {
            Log.e(TAG, "Glide error loading single image", e);
            showProgress(false);
        }
    }

    // ── Image loading ──

    private void loadImage(@NonNull Object model, @NonNull ImageView target, boolean isBefore) {
        try {
            Glide.with(getContext())
                    .load(model)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object m,
                                                    @NonNull Target<Drawable> t, boolean isFirst) {
                            Log.e(TAG, (isBefore ? "Before" : "After") + " image load failed", e);
                            if (isBefore) beforeReady = true; else afterReady = true;
                            checkBothReady();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object m,
                                                       @NonNull Target<Drawable> t, @NonNull DataSource ds,
                                                       boolean isFirst) {
                            if (isBefore) beforeReady = true; else afterReady = true;
                            checkBothReady();
                            return false;
                        }
                    })
                    .into(target);
        } catch (Exception e) {
            Log.e(TAG, "Glide error loading " + (isBefore ? "before" : "after") + " image", e);
            if (isBefore) beforeReady = true; else afterReady = true;
            checkBothReady();
        }
    }

    // ── Internal ──

    private void checkBothReady() {
        if (beforeReady && afterReady) {
            showProgress(false);
            revealSliderUI();
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? VISIBLE : GONE);
        }
    }

    private void hideSliderUI() {
        if (sliderLine != null) sliderLine.setVisibility(INVISIBLE);
        if (sliderHandle != null) sliderHandle.setVisibility(INVISIBLE);
        if (tvBeforeLabel != null) tvBeforeLabel.setVisibility(GONE);
        if (tvAfterLabel != null) tvAfterLabel.setVisibility(GONE);
    }

    private void revealSliderUI() {
        if (sliderLine != null) {
            sliderLine.setVisibility(VISIBLE);
            sliderLine.setAlpha(0f);
            sliderLine.animate().alpha(1f).setDuration(300).start();
        }
        if (sliderHandle != null) {
            sliderHandle.setVisibility(VISIBLE);
            sliderHandle.setAlpha(0f);
            sliderHandle.setScaleX(0.5f);
            sliderHandle.setScaleY(0.5f);
            sliderHandle.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(350)
                    .start();
        }
        if (tvBeforeLabel != null) {
            tvBeforeLabel.setVisibility(VISIBLE);
            tvBeforeLabel.setAlpha(0f);
            tvBeforeLabel.animate().alpha(1f).setDuration(300).setStartDelay(150).start();
        }
        if (tvAfterLabel != null) {
            tvAfterLabel.setVisibility(VISIBLE);
            tvAfterLabel.setAlpha(0f);
            tvAfterLabel.animate().alpha(1f).setDuration(300).setStartDelay(150).start();
        }

        // Apply current slider position
        moveSliderTo(sliderPosition);
    }

    /**
     * Moves the slider to the given position and clips the AFTER image.
     * <p>
     * Position 0.0 → AFTER fully hidden (all BEFORE visible)
     * Position 0.5 → half/half (default)
     * Position 1.0 → AFTER fully visible (BEFORE hidden)
     * <p>
     * The AFTER image is clipped with a Rect that starts at the slider's X position
     * and extends to the full width. This means the portion LEFT of the slider
     * shows the BEFORE image underneath, and the portion RIGHT of the slider
     * shows the AFTER image on top.
     */
    private void moveSliderTo(float position) {
        sliderPosition = Math.max(0f, Math.min(1f, position));
        if (viewWidth <= 0) return;

        int sliderX = (int) (sliderPosition * viewWidth);
        int h = viewHeight > 0 ? viewHeight : getHeight();
        if (h <= 0) return; // View not measured yet — skip to avoid invisible clip rect

        // Clip the AFTER image: only show from sliderX to the right edge.
        // This reveals BEFORE on the left, AFTER on the right.
        if (ivAfter != null) {
            Rect clipRect = new Rect(sliderX, 0, viewWidth, h);
            ivAfter.setClipBounds(clipRect);
        }

        // Position the divider line and handle at the slider edge
        float xPos = (float) sliderX;
        if (sliderLine != null) {
            sliderLine.setTranslationX(xPos - (viewWidth / 2f));
        }
        if (sliderHandle != null) {
            sliderHandle.setTranslationX(xPos - (viewWidth / 2f));
        }

        // Fade labels when slider is near the edges
        updateLabelsAlpha(xPos);
    }

    private void updateLabelsAlpha(float xPos) {
        float fadeThreshold = 120f;
        if (tvBeforeLabel != null) {
            tvBeforeLabel.setAlpha(xPos < fadeThreshold ? 0f : 1f);
        }
        if (tvAfterLabel != null) {
            tvAfterLabel.setAlpha(xPos > (viewWidth - fadeThreshold) ? 0f : 1f);
        }
    }
}
