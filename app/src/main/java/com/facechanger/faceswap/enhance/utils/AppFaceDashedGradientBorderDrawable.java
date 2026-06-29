package com.facechanger.faceswap.enhance.utils;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Rounded-rect border with a dashed stroke and a linear gradient along the view bounds.
 * XML {@code <shape>} cannot combine {@code stroke} dashGap/dashWidth with a gradient color.
 */
public class AppFaceDashedGradientBorderDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float cornerRadiusPx;
    private final float strokeWidthPx;
    private final float dashWidthPx;
    private final float dashGapPx;
    private final int startColor;
    private final int endColor;

    /**
     * @param cornerRadiusDp matches {@code bg_dashed_border} corners (12dp)
     * @param strokeWidthDp  matches 1.5dp stroke
     * @param dashWidthDp    matches 6dp dash
     * @param dashGapDp      matches 4dp gap
     */
    public AppFaceDashedGradientBorderDrawable(
            @NonNull Resources res,
            int startColor,
            int endColor,
            float cornerRadiusDp,
            float strokeWidthDp,
            float dashWidthDp,
            float dashGapDp) {
        this.startColor = startColor;
        this.endColor = endColor;
        float density = res.getDisplayMetrics().density;
        this.cornerRadiusPx = cornerRadiusDp * density;
        this.strokeWidthPx = strokeWidthDp * density;
        this.dashWidthPx = dashWidthDp * density;
        this.dashGapPx = dashGapDp * density;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidthPx);
        paint.setPathEffect(new DashPathEffect(new float[]{dashWidthPx, dashGapPx}, 0));
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        path.reset();
        if (bounds.isEmpty()) {
            return;
        }
        RectF rect = new RectF(bounds);
        float inset = strokeWidthPx / 2f;
        rect.inset(inset, inset);
        path.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);

        paint.setShader(new LinearGradient(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
                startColor,
                endColor,
                Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
