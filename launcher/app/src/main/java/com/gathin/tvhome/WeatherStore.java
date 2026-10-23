package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The weather the home screen shows and the sky follows: one place from the TV's public address
 * (ipwho.is, geojs.io behind it), one Open-Meteo forecast and one air quality answer for it, all
 * on a background thread and cached in {@code weather.json} so a boot without network still shows
 * yesterday's sky.
 *
 * Nothing polls: Home asks for a refresh on resume and once a minute, and the store answers from
 * the cache until half an hour has passed. Every failure keeps the last good snapshot; the widgets
 * and the backdrop simply wait for the next one.
 */
final class WeatherStore {

    private static final String TAG = "TvHome.Weather";
    static final String CACHE_FILE = "weather.json";
    /** Half an hour between two asks of Open-Meteo; the cache answers in between. */
    static final long TTL_MS = 30 * 60_000L;
    private static final int TIMEOUT_MS = 8_000;
    /** No answer is ever bigger than a few KB; a service answering more is not the service. */
    private static final int MAX_BODY = 256 * 1024;

    /** Everything the widgets and the sky paint, or null fields where a fetch has not landed yet. */
    static final class Snapshot {
        final String city;
        final double lat;
        final double lon;
        /** Null until a forecast has been read. */
        final OpenMeteo.Forecast forecast;
        /** Null until an air quality answer has been read. */
        final OpenMeteo.Air air;
        /** The sky condition of the forecast, {@link OpenMeteo#COND_CLEAR} without one. */
        final int condition;
        final long fetchedAt;

        Snapshot(String city, double lat, double lon, OpenMeteo.Forecast forecast, OpenMeteo.Air air, long fetchedAt) {
            this.city = city;
            this.lat = lat;
            this.lon = lon;
            this.forecast = forecast;
            this.air = air;
            this.condition = forecast != null ? OpenMeteo.condition(forecast.code) : OpenMeteo.COND_CLEAR;
            this.fetchedAt = fetchedAt;
        }
    }

    interface Listener {
        void onWeather(Snapshot snapshot);
    }

    private final Context ctx;
    private final Handler main;
    private final Listener listener;
    private final File cache;

    private volatile Snapshot current;
    private HandlerThread thread;
    private volatile Handler bg;
    private long lastFetch;
    private boolean fetching;

    WeatherStore(Context ctx, Handler main, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.main = main;
        this.listener = listener;
        this.cache = new File(this.ctx.getFilesDir(), CACHE_FILE);
        this.current = loadCache();
    }

    /** The last snapshot, from the cache or a fetch; never null, fields may be. */
    Snapshot current() {
        return current;
    }

    /** Starts the background thread; the cache is already in {@link #current()}. */
    void start() {
        if (thread != null) return;
        thread = new HandlerThread("weather");
        thread.start();
        bg = new Handler(thread.getLooper());
        refreshIfStale();
    }

    void stop() {
        if (thread == null) return;
        bg.removeCallbacksAndMessages(null);
        thread.quit();
        thread = null;
        bg = null;
        fetching = false;
    }

    /** Asks for a fetch when the cache is older than the TTL and none is running. Cheap per minute. */
    void refreshIfStale() {
        Handler b = bg;
        if (b == null) return;
        long now = System.currentTimeMillis();
        if (fetching || now - lastFetch < TTL_MS) return;
        lastFetch = now;
        fetching = true;
        b.post(this::fetch);
    }

    // ---- background ------------------------------------------------------------------------------

    private void fetch() {
        Snapshot done = null;
        try {
            done = fetchLocked();
        } catch (Exception e) {
            Log.w(TAG, "fetch failed: " + e);
        } finally {
            fetching = false;
        }
        if (done == null) return;
        current = done;
        saveCache(done);
        final Snapshot result = done;
        main.post(() -> listener.onWeather(result));
    }

    private Snapshot fetchLocked() {
        Snapshot base = current;
        String city = base != null ? base.city : "";
        double lat = base != null ? base.lat : Double.NaN;
        double lon = base != null ? base.lon : Double.NaN;
        double[] fixed = configuredPlace();
        String label = configuredLabel();
        if (!label.isEmpty()) {
            city = label;
        }
        if (fixed != null) {
            lat = fixed[0];
            lon = fixed[1];
        } else if (Double.isNaN(lat) || Double.isNaN(lon)) {
            Geo.Place place = geolocate();
            if (place == null) return null;
            lat = place.lat;
            lon = place.lon;
            if (label.isEmpty()) city = place.city;
        }
        OpenMeteo.Forecast forecast = OpenMeteo.parseForecast(get(OpenMeteo.forecastUrl(lat, lon)));
        if (forecast == null) return null;
        // Air quality is a bonus: a failed air endpoint keeps the weather, not the other way round.
        OpenMeteo.Air air = OpenMeteo.parseAir(get(OpenMeteo.airUrl(lat, lon)));
        return new Snapshot(city, lat, lon, forecast, air, System.currentTimeMillis());
    }

    /** The place from config.xml (latitude/longitude), or null when the TV geolocates itself. */
    private double[] configuredPlace() {
        try {
            String lat = ctx.getString(R.string.latitude).trim();
            String lon = ctx.getString(R.string.longitude).trim();
            if (lat.isEmpty() || lon.isEmpty()) return null;
            return new double[]{Double.parseDouble(lat), Double.parseDouble(lon)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String configuredLabel() {
        return ctx.getString(R.string.weather_label).trim();
    }

    private Geo.Place geolocate() {
        Geo.Place p = Geo.parseIpWho(get(Geo.IPWHO_URL));
        return p != null ? p : Geo.parseGeoJs(get(Geo.GEOJS_URL));
    }

    /** The body of one GET, or null on any trouble. Redirects stay inside the service's own host. */
    private static String get(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(TIMEOUT_MS);
            con.setReadTimeout(TIMEOUT_MS);
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept", "application/json");
            if (con.getResponseCode() != 200) return null;
            try (InputStream in = con.getInputStream()) {
                byte[] buf = new byte[8192];
                int n = 0, r;
                while (n < MAX_BODY && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
                return n >= MAX_BODY ? null : new String(buf, 0, n, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    // ---- cache -----------------------------------------------------------------------------------

    private Snapshot loadCache() {
        if (!cache.isFile()) return null;
        try {
            String text = new String(new AtomicFile(cache).readFully(), StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(text);
            if (o.optInt("v", 0) != 1) return null;
            OpenMeteo.Forecast f = null;
            if (o.has("code")) {
                f = new OpenMeteo.Forecast(o.optDouble("temp", Double.NaN), o.optDouble("max", Double.NaN),
                        o.optDouble("min", Double.NaN), o.optInt("code", 0), o.optBoolean("day", true));
                if (Double.isNaN(f.temp)) f = null;
            }
            OpenMeteo.Air air = o.has("aqi") ? new OpenMeteo.Air(o.optInt("aqi", -1), o.optDouble("pm25", -1)) : null;
            if (air != null && air.aqi < 0) air = null;
            return new Snapshot(o.optString("city", ""), o.optDouble("lat", Double.NaN),
                    o.optDouble("lon", Double.NaN), f, air, o.optLong("t", 0));
        } catch (Exception e) {
            Log.w(TAG, "cache unreadable, starting without it: " + e);
            return null;
        }
    }

    private void saveCache(Snapshot s) {
        try {
            JSONObject o = new JSONObject();
            o.put("v", 1);
            o.put("t", s.fetchedAt);
            o.put("city", s.city);
            if (!Double.isNaN(s.lat)) o.put("lat", s.lat);
            if (!Double.isNaN(s.lon)) o.put("lon", s.lon);
            if (s.forecast != null) {
                o.put("temp", s.forecast.temp);
                o.put("max", s.forecast.max);
                o.put("min", s.forecast.min);
                o.put("code", s.forecast.code);
                o.put("day", s.forecast.day);
            }
            if (s.air != null) {
                o.put("aqi", s.air.aqi);
                o.put("pm25", s.air.pm25);
            }
            AtomicFile af = new AtomicFile(cache);
            try (FileOutputStream out = af.startWrite()) {
                out.write(o.toString().getBytes(StandardCharsets.UTF_8));
                af.finishWrite(out);
            }
        } catch (Exception e) {
            Log.w(TAG, "cache not written: " + e);
        }
    }
}
