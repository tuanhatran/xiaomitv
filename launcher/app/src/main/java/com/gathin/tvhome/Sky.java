package com.gathin.tvhome;

/**
 * Sky palette for the optional backdrop (config.xml, sky_intensity): the dark time-of-day
 * gradient the launcher ships with, and a light one for light skins, each bent by the weather
 * the launcher last fetched (config.xml, weather_sky) so rain greys the horizon and snow pales
 * it instead of the backdrop ignoring the sky outside.
 *
 * Pure maths, no Android types and no clock of its own: the caller passes minutes since midnight
 * and the condition, so the palette can be tested on the JVM. The colours are interpolated between
 * a handful of anchor points through the day, which is what makes the backdrop drift from night
 * to dawn to sunset instead of switching at fixed hours. The dark palette stays dark enough for
 * white text; the light one stays light enough for dark ink.
 */
final class Sky {

    /** One anchor: a minute of the day and the values the palette takes exactly there. */
    static final class Stop {
        final int minute;
        final int top;
        final int mid;
        final int bottom;
        /** Alpha of the sun/moon glow near the horizon, 0 to 255. */
        final int sun;
        /** Horizontal position of the glow, 0 = left edge, 1 = right edge. */
        final float sunX;
        /** The two soft blobs a light sky lays over the gradient; 0 keeps them out. */
        final int blobA;
        final int blobB;

        Stop(int minute, int top, int mid, int bottom, int sun, float sunX, int blobA, int blobB) {
            this.minute = minute;
            this.top = top;
            this.mid = mid;
            this.bottom = bottom;
            this.sun = sun;
            this.sunX = sunX;
            this.blobA = blobA;
            this.blobB = blobB;
        }
    }

    /** The palette at one instant: the three gradient colours, the glow, the blobs and the precipitation. */
    static final class Palette {
        final int top;
        final int mid;
        final int bottom;
        final int sun;
        final float sunX;
        final int blobA;
        final int blobB;
        /** 0 none, 1 rain streaks, 2 snow flakes, for the backdrop to draw. */
        final int precip;

        Palette(int top, int mid, int bottom, int sun, float sunX, int blobA, int blobB, int precip) {
            this.top = top;
            this.mid = mid;
            this.bottom = bottom;
            this.sun = sun;
            this.sunX = sunX;
            this.blobA = blobA;
            this.blobB = blobB;
            this.precip = precip;
        }
    }

    private static final int NIGHT_TOP = 0xFF04060E;
    private static final int NIGHT_MID = 0xFF070C1A;
    private static final int NIGHT_BOTTOM = 0xFF0B1326;

    // Anchors were picked around a temperate evening: full night until half past five, dawn before
    // seven, the warmest glow between seven in the evening and a quarter to eight. With a location
    // set, at(minute, sunrise, sunset) stretches this reference day onto the real one.
    static final int REF_SUNRISE = 450;
    static final int REF_SUNSET = 1140;
    private static final Stop[] STOPS = {
            new Stop(0, NIGHT_TOP, NIGHT_MID, NIGHT_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(320, NIGHT_TOP, NIGHT_MID, NIGHT_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(390, 0xFF160F2E, 0xFF3B2352, 0xFF8A4A6B, 100, 0.24f, 0, 0),
            new Stop(450, 0xFF12203F, 0xFF5C3A63, 0xFFD3674F, 160, 0.30f, 0, 0),
            new Stop(570, 0xFF0E1A33, 0xFF1F3557, 0xFF43648B, 90, 0.38f, 0, 0),
            new Stop(780, 0xFF0B1524, 0xFF17304F, 0xFF31587F, 60, 0.50f, 0, 0),
            new Stop(1020, 0xFF0D1526, 0xFF20395C, 0xFF4C5470, 90, 0.62f, 0, 0),
            new Stop(1140, 0xFF1B1030, 0xFF6C2E52, 0xFFE2704A, 190, 0.76f, 0, 0),
            new Stop(1185, 0xFF140C27, 0xFF4A2049, 0xFFA54A5C, 130, 0.82f, 0, 0),
            new Stop(1260, 0xFF0A0A1C, 0xFF191230, 0xFF3A1F49, 60, 0.86f, 0, 0),
            new Stop(1350, NIGHT_TOP, NIGHT_MID, NIGHT_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(1440, NIGHT_TOP, NIGHT_MID, NIGHT_BOTTOM, 0, 0.30f, 0, 0),
    };

    // The light skin's day: a pale green room with two soft blobs, warm at the horizon when the sun
    // is low, and a deep green night so a light-skinned Home still rests the eyes after dark.
    private static final int LN_TOP = 0xFF0D1F15;
    private static final int LN_MID = 0xFF12291B;
    private static final int LN_BOTTOM = 0xFF173322;
    private static final int BLOB_A = 0xFFCFE7BC;   // the large green blob, right of centre
    private static final int BLOB_B = 0xFFF6E2C0;   // the warm one, low left
    private static final Stop[] LIGHT_STOPS = {
            new Stop(0, LN_TOP, LN_MID, LN_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(320, LN_TOP, LN_MID, LN_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(390, 0xFF9DBE93, 0xFFC4D8AE, 0xFFE8D9AE, 110, 0.24f, 0xFFB7D3A4, 0xFFEFD9AE),
            new Stop(450, 0xFFB4D0A4, 0xFFD3E4C0, 0xFFF2E6C2, 150, 0.30f, 0xFFC2DBAC, 0xFFF3E2BC),
            new Stop(570, 0xFFD3E6C4, 0xFFE4F0D6, 0xFFF6FAEE, 70, 0.38f, 0xFFCFE7BC, 0xFFF6E2C0),
            new Stop(780, 0xFFDDEDD0, 0xFFEBF4DF, 0xFFF8FBF2, 45, 0.50f, 0xFFCFE7BC, 0xFFF6E2C0),
            new Stop(1020, 0xFFD8E9C9, 0xFFE8F2DA, 0xFFF7FAEF, 65, 0.62f, 0xFFCFE7BC, 0xFFF6E2C0),
            new Stop(1140, 0xFFC8DFB2, 0xFFE2E8C4, 0xFFF5E0B8, 150, 0.76f, 0xFFC4DCA9, 0xFFF4DCAE),
            new Stop(1185, 0xFFAFCBA0, 0xFFD2DDB4, 0xFFEDD3AC, 100, 0.82f, 0xFFB7D09E, 0xFFEFD3A6),
            new Stop(1260, 0xFF3A5741, 0xFF47654C, 0xFF527057, 40, 0.86f, 0, 0),
            new Stop(1350, LN_TOP, LN_MID, LN_BOTTOM, 0, 0.30f, 0, 0),
            new Stop(1440, LN_TOP, LN_MID, LN_BOTTOM, 0, 0.30f, 0, 0),
    };

    private Sky() {
    }

    /**
     * The palette at {@code minuteOfDay} on a day whose sun rises at {@code sunrise} and sets at
     * {@code sunset} (minutes of the day): night, day and evening are each stretched piecewise so
     * the reference sunrise and sunset glows land on the real ones.
     */
    static Palette at(int minuteOfDay, int sunrise, int sunset) {
        return at(minuteOfDay, sunrise, sunset, OpenMeteo.COND_CLEAR, false);
    }

    /** The palette at {@code minuteOfDay}, clamped to a day. */
    static Palette at(int minuteOfDay) {
        return at(minuteOfDay, 0, 1440, OpenMeteo.COND_CLEAR, false);
    }

    /**
     * The palette for a sky of one skin and one weather condition. {@code condition} is one of the
     * {@code OpenMeteo.COND_*} values; anything unknown paints as clear.
     */
    static Palette at(int minuteOfDay, int sunrise, int sunset, int condition, boolean light) {
        int m = Math.max(0, Math.min(1440, minuteOfDay));
        if (sunrise > 0 && sunset < 1440 && sunrise < sunset) {
            float ref;
            if (m < sunrise) ref = m * REF_SUNRISE / (float) sunrise;
            else if (m < sunset) ref = REF_SUNRISE + (m - sunrise) * (REF_SUNSET - REF_SUNRISE) / (float) (sunset - sunrise);
            else ref = REF_SUNSET + (m - sunset) * (1440 - REF_SUNSET) / (float) (1440 - sunset);
            m = Math.round(ref);
        }
        Stop[] stops = light ? LIGHT_STOPS : STOPS;
        m = Math.max(0, Math.min(1440, m));
        Stop a = stops[0];
        Stop b = stops[stops.length - 1];
        for (int i = 0; i < stops.length - 1; i++) {
            if (m >= stops[i].minute && m <= stops[i + 1].minute) {
                a = stops[i];
                b = stops[i + 1];
                break;
            }
        }
        float t = b.minute == a.minute ? 0f : (m - a.minute) / (float) (b.minute - a.minute);
        Palette p = new Palette(
                lerpColor(a.top, b.top, t),
                lerpColor(a.mid, b.mid, t),
                lerpColor(a.bottom, b.bottom, t),
                lerp(a.sun, b.sun, t),
                lerp(a.sunX, b.sunX, t),
                a.blobA == 0 || b.blobA == 0 ? 0 : lerpColor(a.blobA, b.blobA, t),
                a.blobB == 0 || b.blobB == 0 ? 0 : lerpColor(a.blobB, b.blobB, t),
                0);
        return weather(p, condition, light);
    }

    /**
     * Bends a clear palette towards the sky of one condition: overcast greys it, rain greys it
     * further and asks for streaks, snow pales it and asks for flakes, thunder takes the light
     * out of it. Clear returns the palette untouched.
     */
    static Palette weather(Palette p, int condition, boolean light) {
        switch (condition) {
            case OpenMeteo.COND_PARTLY:
                return mix(p, light ? 0xFFDDE7D6 : 0xFF1A2129, 0.10f, 0.85f, 0.10f, 0);
            case OpenMeteo.COND_CLOUDY:
                return mix(p, light ? 0xFFDBE3D9 : 0xFF20262D, 0.30f, 0.40f, 0.45f, 0);
            case OpenMeteo.COND_FOG:
                return mix(p, light ? 0xFFE5EAE4 : 0xFF2A3232, 0.55f, 0f, 0.70f, 0);
            case OpenMeteo.COND_RAIN:
                return mix(p, light ? 0xFFD3DED6 : 0xFF151C22, 0.45f, 0.15f, 0.55f, 1);
            case OpenMeteo.COND_SNOW:
                return mix(p, light ? 0xFFEEF3F5 : 0xFF2D3742, 0.50f, 0.50f, 0.60f, 2);
            case OpenMeteo.COND_THUNDER:
                return mix(p, light ? 0xFFC2CDC4 : 0xFF0F141B, 0.55f, 0.05f, 0.55f, 1);
            default:
                return p;
        }
    }

    /** Every colour of the palette pulled {@code t} towards {@code target}; the glow scaled, blobs pulled, precipitation set. */
    private static Palette mix(Palette p, int target, float t, float sunScale, float blobT, int precip) {
        return new Palette(
                lerpColor(p.top, target, t),
                lerpColor(p.mid, target, t),
                lerpColor(p.bottom, target, t),
                Math.round(p.sun * sunScale),
                p.sunX,
                p.blobA == 0 ? 0 : lerpColor(p.blobA, target, blobT),
                p.blobB == 0 ? 0 : lerpColor(p.blobB, target, blobT),
                precip);
    }

    /** Minute of day, 0 to 1439, of a wall clock time. */
    static int minuteOfDay(int hour, int minute) {
        return Math.max(0, Math.min(23, hour)) * 60 + Math.max(0, Math.min(59, minute));
    }

    /** Blends two opaque ARGB colours channel by channel; {@code t} is 0 to 1. */
    static int lerpColor(int from, int to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float t) {
        return Math.round(from + (to - from) * Math.max(0f, Math.min(1f, t)));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * Math.max(0f, Math.min(1f, t));
    }
}
