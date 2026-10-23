package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;

/**
 * A full-screen gradient that follows the clock: night blues, a warm horizon at sunset, a soft
 * glow that travels with the sun. Off by default (config.xml, sky_intensity = 0); a theme turns
 * it on with {@code "sky_intensity": 60}.
 *
 * Like the ambient glow, the backdrop is one small bitmap (480x270) stretched over the screen:
 * a gradient repainted at full resolution every minute is exactly the CPU cost the ambient glow
 * was rewritten to avoid on weak GPUs. It is redrawn once a minute and never animated, so the
 * cost is a single full-screen draw per minute.
 */
public class SkyView extends View {

    /** Full-screen and faded with setAlpha: without this every redraw of Home goes through an offscreen layer. */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static final int TEX_W = 480;
    private static final int TEX_H = 270;

    /** Opacity of the whole backdrop, 0 to 255, from sky_intensity. */
    private int intensity;
    /** The light skin's pale palette instead of the dark one (config.xml light_skin). */
    private boolean light;
    /** The last fetched weather condition, or -1 to paint a clear sky. */
    private int condition = -1;
    private int lastMinute = -1;
    private Bitmap texture;
    private BitmapDrawable drawable;

    public SkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /** {@code percent} of sky_intensity: 0 hides the backdrop, 100 covers the wallpaper. */
    public void setIntensity(int percent) {
        intensity = Math.max(0, Math.min(100, percent));
        setAlpha(intensity / 100f);
        setVisibility(intensity == 0 ? GONE : VISIBLE);
        if (intensity > 0) render(true);
    }

    /** Paints the light palette; a theme's light_skin decides, once at start. */
    public void setLight(boolean light) {
        if (this.light == light) return;
        this.light = light;
        render(true);
    }

    /** Bends the palette towards this weather (weather_sky); -1 returns it to clear. */
    public void setCondition(int condition) {
        if (this.condition == condition) return;
        this.condition = condition;
        render(true);
    }

    /** Picks up a new time of day; cheap and safe to call every minute. */
    public void update() {
        if (intensity == 0 || getVisibility() != VISIBLE) return;
        render(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (intensity > 0) render(true);
    }

    /** Today's sunrise and sunset at the configured place, or null without one (fixed reference day). */
    @Nullable
    private int[] sunTimes() {
        String lat = getContext().getString(R.string.latitude);
        String lon = getContext().getString(R.string.longitude);
        if (lat.isEmpty() || lon.isEmpty()) return null;
        try {
            return Sun.riseAndSet(LocalDate.now(), Double.parseDouble(lat), Double.parseDouble(lon),
                    ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void render(boolean force) {
        Calendar now = Calendar.getInstance();
        int minute = Sky.minuteOfDay(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        if (!force && minute == lastMinute) return;
        lastMinute = minute;

        int[] sun = sunTimes();
        int cond = condition >= 0 ? condition : OpenMeteo.COND_CLEAR;
        Sky.Palette sky = sun == null
                ? Sky.at(minute, 0, 1440, cond, light)
                : Sky.at(minute, sun[0], sun[1], cond, light);
        if (texture == null) texture = Bitmap.createBitmap(TEX_W, TEX_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(texture);
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0f, 0f, 0f, TEX_H,
                new int[]{sky.top, sky.mid, sky.bottom},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);

        if (sky.sun > 0) {
            // A warm core low on the horizon, where the sun (or its afterglow) sits.
            int warm = Sky.lerpColor(sky.bottom, 0xFFFFD9A0, 0.65f);
            paint.setShader(new RadialGradient(
                    sky.sunX * TEX_W, TEX_H * 0.80f, TEX_W * 0.55f,
                    new int[]{(sky.sun << 24) | (warm & 0x00FFFFFF), warm & 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);
        }

        // The light palette's two soft blobs: a wide wash of colour over the gradient, no edge.
        if (sky.blobA != 0) blob(canvas, paint, sky.blobA, TEX_W * 0.70f, TEX_H * 0.30f, TEX_W * 0.55f);
        if (sky.blobB != 0) blob(canvas, paint, sky.blobB, TEX_W * 0.22f, TEX_H * 0.78f, TEX_W * 0.45f);

        // Rain or snow, drawn once a minute onto the same small texture the gradient uses.
        if (sky.precip != 0) precipitation(canvas, sky.precip, light);

        if (drawable == null) {
            drawable = new BitmapDrawable(getResources(), texture);
            drawable.setGravity(Gravity.FILL);
            setBackground(drawable);
        }
        invalidate();
    }

    private static void blob(Canvas canvas, Paint paint, int colour, float cx, float cy, float r) {
        paint.setShader(new RadialGradient(cx, cy, r,
                new int[]{(colour & 0x00FFFFFF) | 0x99000000, colour & 0x00FFFFFF},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);
        paint.setShader(null);
    }

    /** A fixed scatter of streaks or flakes: the same every minute, so the backdrop never crawls. */
    private static void precipitation(Canvas canvas, int kind, boolean light) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // White reads on the dark palette; the light one needs slate rain, and snow stays white.
        if (kind == 2) paint.setColor(light ? 0x99FFFFFF : 0xB3FFFFFF);
        else paint.setColor(light ? 0x33506A78 : 0x59FFFFFF);
        long seed = 20240101L;
        for (int i = 0; i < (kind == 2 ? 90 : 70); i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float x = ((seed >>> 40) % TEX_W);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float y = ((seed >>> 40) % TEX_H);
            if (kind == 2) {
                paint.setStrokeWidth(0f);
                canvas.drawCircle(x, y, 1.4f, paint);
            } else {
                paint.setStrokeWidth(1.5f);
                canvas.drawLine(x, y, x - 3f, y + 9f, paint);
            }
        }
    }
}
