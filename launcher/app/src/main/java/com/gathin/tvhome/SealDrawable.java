package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * The air quality widget's seal: a disc with small bumps around its edge, filled with the colour
 * of the AQI band (Aqi). Drawn, not a bitmap, so a band change is one repaint and the APK carries
 * no image per band.
 */
final class SealDrawable extends Drawable {

    private static final int BUMPS = 16;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    SealDrawable(int colour) {
        paint.setColor(colour);
    }

    void setColour(int colour) {
        paint.setColor(colour);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        float r = Math.min(getBounds().width(), getBounds().height()) / 2f;
        if (r <= 0f) return;
        float cx = getBounds().exactCenterX();
        float cy = getBounds().exactCenterY();
        canvas.drawCircle(cx, cy, r * 0.86f, paint);
        float bump = r * 0.17f;
        float ring = r - bump;
        for (int i = 0; i < BUMPS; i++) {
            double a = i * 2 * Math.PI / BUMPS;
            canvas.drawCircle(cx + (float) (Math.cos(a) * ring), cy + (float) (Math.sin(a) * ring), bump, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
