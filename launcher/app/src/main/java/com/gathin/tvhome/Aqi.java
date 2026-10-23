package com.gathin.tvhome;

/**
 * The US AQI bands the air quality widget names and colours: the index the Open-Meteo air
 * endpoint returns as {@code us_aqi}. Pure, so the bands stay unit tested.
 */
final class Aqi {

    /** Good, moderate, poor (unhealthy for sensitive groups and unhealthy), very poor (beyond). */
    static final int LEVEL_GOOD = 0;
    static final int LEVEL_MODERATE = 1;
    static final int LEVEL_POOR = 2;
    static final int LEVEL_VERY_POOR = 3;

    /** Seal colours per level, deepest green for the cleanest air. */
    private static final int[] COLOUR = {0xFF57830F, 0xFFD9A02B, 0xFFD9642A, 0xFFB3362B};

    private Aqi() {
    }

    static int level(int usAqi) {
        if (usAqi <= 50) return LEVEL_GOOD;
        if (usAqi <= 100) return LEVEL_MODERATE;
        if (usAqi <= 200) return LEVEL_POOR;
        return LEVEL_VERY_POOR;
    }

    static int colour(int level) {
        return COLOUR[Math.max(0, Math.min(COLOUR.length - 1, level))];
    }
}
