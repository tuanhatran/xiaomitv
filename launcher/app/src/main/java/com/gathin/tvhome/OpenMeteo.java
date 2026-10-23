package com.gathin.tvhome;

import org.json.JSONObject;

/**
 * The Open-Meteo answers the launcher reads: current weather with the day's extremes, and the
 * current air quality. Parsing is pure (a JSON text in, a value or null out) so it runs in JVM
 * tests; nothing here opens a connection.
 *
 * Both endpoints are keyless and CORS-free, and the forecast carries the WMO weather code the
 * sky backdrop and the widget icon share.
 */
final class OpenMeteo {

    /** Sky conditions, coarse enough to paint a backdrop: see {@link #condition(int)}. */
    static final int COND_CLEAR = 0;
    static final int COND_PARTLY = 1;
    static final int COND_CLOUDY = 2;
    static final int COND_FOG = 3;
    static final int COND_RAIN = 4;
    static final int COND_SNOW = 5;
    static final int COND_THUNDER = 6;

    /** Current weather plus today's extremes; temperatures in degrees Celsius. */
    static final class Forecast {
        final double temp;
        final double max;
        final double min;
        /** WMO weather code. */
        final int code;
        /** True while the sun is up at the place. */
        final boolean day;

        Forecast(double temp, double max, double min, int code, boolean day) {
            this.temp = temp;
            this.max = max;
            this.min = min;
            this.code = code;
            this.day = day;
        }
    }

    /** Current air quality: the US AQI and fine particulates in micrograms per cubic metre. */
    static final class Air {
        final int aqi;
        final double pm25;

        Air(int aqi, double pm25) {
            this.aqi = aqi;
            this.pm25 = pm25;
        }
    }

    private OpenMeteo() {
    }

    static String forecastUrl(double lat, double lon) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + clamp(lat, 90) + "&longitude=" + clamp(lon, 180)
                + "&current=temperature_2m,is_day,weather_code"
                + "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto";
    }

    static String airUrl(double lat, double lon) {
        return "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + clamp(lat, 90)
                + "&longitude=" + clamp(lon, 180) + "&current=us_aqi,pm2_5&timezone=auto";
    }

    /** Three decimals, a dot, no exponent, inside the coordinate range: what a URL can carry. */
    private static String clamp(double v, int bound) {
        return String.format(java.util.Locale.US, "%.3f", Math.max(-bound, Math.min(bound, v)));
    }

    /** The forecast of one response, or null when a field the widget needs is missing or not a number. */
    static Forecast parseForecast(String json) {
        try {
            JSONObject o = new JSONObject(json);
            JSONObject cur = o.optJSONObject("current");
            JSONObject daily = o.optJSONObject("daily");
            if (cur == null || daily == null) return null;
            double temp = cur.optDouble("temperature_2m", Double.NaN);
            int code = cur.optInt("weather_code", -1);
            int isDay = cur.optInt("is_day", 1);
            double max = daily.optJSONArray("temperature_2m_max") != null
                    ? daily.optJSONArray("temperature_2m_max").optDouble(0, Double.NaN) : Double.NaN;
            double min = daily.optJSONArray("temperature_2m_min") != null
                    ? daily.optJSONArray("temperature_2m_min").optDouble(0, Double.NaN) : Double.NaN;
            if (Double.isNaN(temp) || Double.isNaN(max) || Double.isNaN(min) || code < 0) return null;
            return new Forecast(temp, max, min, code, isDay != 0);
        } catch (Exception e) {
            return null;
        }
    }

    /** The air quality of one response, or null without a usable US AQI. PM2.5 may be -1 when absent. */
    static Air parseAir(String json) {
        try {
            JSONObject cur = new JSONObject(json).optJSONObject("current");
            if (cur == null) return null;
            double aqi = cur.optDouble("us_aqi", Double.NaN);
            if (Double.isNaN(aqi) || aqi < 0) return null;
            return new Air((int) Math.round(aqi), cur.optDouble("pm2_5", -1));
        } catch (Exception e) {
            return null;
        }
    }

    /** The sky condition a WMO weather code paints as. */
    static int condition(int wmoCode) {
        if (wmoCode == 0) return COND_CLEAR;
        if (wmoCode == 1 || wmoCode == 2) return COND_PARTLY;
        if (wmoCode == 3) return COND_CLOUDY;
        if (wmoCode == 45 || wmoCode == 48) return COND_FOG;
        if (wmoCode >= 71 && wmoCode <= 77 || wmoCode == 85 || wmoCode == 86) return COND_SNOW;
        if (wmoCode >= 95) return COND_THUNDER;
        if (wmoCode >= 51 && wmoCode <= 67 || wmoCode >= 80 && wmoCode <= 82) return COND_RAIN;
        return COND_CLOUDY;
    }
}
