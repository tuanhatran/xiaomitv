package com.gathin.tvhome;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Binds the widget cards between the header and the grid to their data. Home owns one instance,
 * hands it the row from the layout, and calls the setters as the WeatherStore, the SystemStats
 * reader and the clock deliver new values; the binder paints nothing until then, so a card shows
 * only what has arrived.
 *
 * Which cards exist is the config's {@code widgets} list: a card not listed stays gone and takes
 * no space, and a row with none of them stays gone whole. Nothing here is focusable, so the cards
 * never enter the D-pad path.
 */
final class Widgets {

    private static final int[] COND_ICON_DAY = {
            R.drawable.cond_sun, R.drawable.cond_partly, R.drawable.cond_cloud, R.drawable.cond_fog,
            R.drawable.cond_rain, R.drawable.cond_snow, R.drawable.cond_thunder};
    private static final int[] COND_ICON_NIGHT = {
            R.drawable.cond_moon, R.drawable.cond_partly, R.drawable.cond_cloud, R.drawable.cond_fog,
            R.drawable.cond_rain, R.drawable.cond_snow, R.drawable.cond_thunder};
    private static final int[] COND_WORD = {
            R.string.cond_clear, R.string.cond_partly, R.string.cond_clouds, R.string.cond_fog,
            R.string.cond_rain, R.string.cond_snow, R.string.cond_thunder};

    private final Context ctx;

    private final View weatherCard;
    private final TextView city;
    private final ImageView icon;
    private final TextView temp;
    private final TextView cond;
    private final TextView range;

    private final View airCard;
    private final SealDrawable seal;
    private final TextView aqi;
    private final TextView level;
    private final TextView pm25;

    private final View systemCard;
    private final View cpuRow;
    private final TextView cpu;
    private final ProgressBar cpuBar;
    private final TextView mem;
    private final ProgressBar memBar;
    private final TextView store;
    private final ProgressBar storeBar;

    private final View lunarCard;
    private final TextView lunarYear;
    private final TextView lunarDate;
    private final TextView lunarExtra;

    Widgets(Context ctx, View row, List<String> widgets, Config cfg) {
        this.ctx = ctx;
        boolean any = false;

        weatherCard = card(row, R.id.wWeatherCard, widgets.contains("weather"));
        city = weatherCard.findViewById(R.id.wWeatherCity);
        icon = weatherCard.findViewById(R.id.wWeatherIcon);
        temp = weatherCard.findViewById(R.id.wWeatherTemp);
        cond = weatherCard.findViewById(R.id.wWeatherCond);
        range = weatherCard.findViewById(R.id.wWeatherRange);
        any |= weatherCard.getVisibility() == View.VISIBLE;

        airCard = card(row, R.id.wAirCard, widgets.contains("air"));
        seal = new SealDrawable(Aqi.colour(Aqi.LEVEL_GOOD));
        airCard.findViewById(R.id.wAirSeal).setBackground(seal);
        aqi = airCard.findViewById(R.id.wAirAqi);
        level = airCard.findViewById(R.id.wAirLevel);
        pm25 = airCard.findViewById(R.id.wAirPm25);
        any |= airCard.getVisibility() == View.VISIBLE;

        systemCard = card(row, R.id.wSystemCard, widgets.contains("system"));
        cpuRow = systemCard.findViewById(R.id.wSysCpuRow);
        cpu = systemCard.findViewById(R.id.wSysCpu);
        cpuBar = systemCard.findViewById(R.id.wSysCpuBar);
        mem = systemCard.findViewById(R.id.wSysMem);
        memBar = systemCard.findViewById(R.id.wSysMemBar);
        store = systemCard.findViewById(R.id.wSysStore);
        storeBar = systemCard.findViewById(R.id.wSysStoreBar);
        any |= systemCard.getVisibility() == View.VISIBLE;

        lunarCard = card(row, R.id.wLunarCard, widgets.contains("lunar"));
        lunarYear = lunarCard.findViewById(R.id.wLunarYear);
        lunarDate = lunarCard.findViewById(R.id.wLunarDate);
        lunarExtra = lunarCard.findViewById(R.id.wLunarExtra);
        any |= lunarCard.getVisibility() == View.VISIBLE;

        row.setVisibility(any ? View.VISIBLE : View.GONE);
        if (any) skin(cfg);
    }

    private static View card(View row, int id, boolean on) {
        View card = row.findViewById(id);
        card.setVisibility(on ? View.VISIBLE : View.GONE);
        return card;
    }

    /** The Config palette over the layout defaults, so a pushed theme recolours the cards too. */
    private void skin(Config cfg) {
        float d = ctx.getResources().getDisplayMetrics().density;
        // Frosted, not solid: the card colour washed paler at the top and thinner at the bottom, so the
        // sky reads through the panel, with a pale rim in place of the band's dark hairline. A real
        // backdrop blur is RenderEffect, which needs API 31; these TVs are on 11, so the gradient is it.
        int base = cfg.cardColor;
        int a = base >>> 24;
        int[] frost = {wash(base, .42f, Math.min(255, a + 20)), wash(base, .04f, Math.max(0, a - 30))};
        int rim = wash(base, .8f, 0x66);
        for (View c : new View[]{weatherCard, airCard, systemCard, lunarCard}) {
            if (c.getVisibility() != View.VISIBLE) continue;
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, frost);
            bg.setCornerRadius(20 * d);
            bg.setStroke(Math.max(1, Math.round(d)), rim);
            c.setBackground(bg);
        }
        city.setTextColor(cfg.inkSoft);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(cfg.ink));
        temp.setTextColor(cfg.ink);
        cond.setTextColor(cfg.inkSoft);
        range.setTextColor(cfg.inkDim);
        ((TextView) airCard.findViewById(R.id.wAirTitle)).setTextColor(cfg.inkSoft);
        pm25.setTextColor(cfg.inkDim);
        ((TextView) systemCard.findViewById(R.id.wSystemTitle)).setTextColor(cfg.inkSoft);
        cpu.setTextColor(cfg.ink);
        mem.setTextColor(cfg.ink);
        store.setTextColor(cfg.ink);
        ((TextView) lunarCard.findViewById(R.id.wLunarTitle)).setTextColor(cfg.inkSoft);
        lunarYear.setTextColor(cfg.ink);
        lunarDate.setTextColor(cfg.inkSoft);
        lunarExtra.setTextColor(cfg.inkDim);
    }

    /** A colour washed f of the way to white, with its alpha replaced. */
    private static int wash(int colour, float f, int alpha) {
        float keep = 1 - f;
        int r = (int) ((colour >> 16 & 255) * keep + 255 * f);
        int g = (int) ((colour >> 8 & 255) * keep + 255 * f);
        int b = (int) ((colour & 255) * keep + 255 * f);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    /** True when the system card is on, so Home keeps the /proc reader running for it. */
    boolean wantsSystem() {
        return systemCard.getVisibility() == View.VISIBLE;
    }

    /** True when any card shows weather or air, so Home runs the WeatherStore. */
    boolean wantsWeather() {
        return weatherCard.getVisibility() == View.VISIBLE || airCard.getVisibility() == View.VISIBLE;
    }

    void setWeather(WeatherStore.Snapshot s) {
        if (s == null) return;
        if (weatherCard.getVisibility() == View.VISIBLE && s.forecast != null) {
            if (!s.city.isEmpty()) city.setText(s.city);
            int c = Math.max(0, Math.min(COND_ICON_DAY.length - 1, s.condition));
            icon.setImageResource(s.forecast.day ? COND_ICON_DAY[c] : COND_ICON_NIGHT[c]);
            temp.setText(Math.round(s.forecast.temp) + "°");
            cond.setText(COND_WORD[c]);
            range.setText(Math.round(s.forecast.max) + "° / " + Math.round(s.forecast.min) + "°");
        }
        if (airCard.getVisibility() == View.VISIBLE && s.air != null) {
            int l = Aqi.level(s.air.aqi);
            seal.setColour(Aqi.colour(l));
            aqi.setText(String.valueOf(s.air.aqi));
            level.setText(aqiLevelWord(l));
            pm25.setText(s.air.pm25 >= 0 ? ctx.getString(R.string.widget_pm25, oneDecimal(s.air.pm25)) : "");
        }
    }

    private String aqiLevelWord(int l) {
        switch (l) {
            case Aqi.LEVEL_MODERATE: return ctx.getString(R.string.aqi_moderate);
            case Aqi.LEVEL_POOR: return ctx.getString(R.string.aqi_poor);
            case Aqi.LEVEL_VERY_POOR: return ctx.getString(R.string.aqi_very_poor);
            default: return ctx.getString(R.string.aqi_good);
        }
    }

    void setSystem(SystemStats.Sample s) {
        if (s == null || systemCard.getVisibility() != View.VISIBLE) return;
        // Android 9 hides the CPU counters from apps on many TVs: drop the row rather than show a
        // figure that never arrives. The first sample is always -1; the reader samples again.
        cpuRow.setVisibility(s.cpu == SystemStats.CPU_UNREADABLE ? View.GONE : View.VISIBLE);
        if (s.cpu >= 0) {
            cpu.setText(ctx.getString(R.string.widget_percent, s.cpu));
            cpuBar.setProgress(s.cpu);
        }
        if (s.memUsedMb >= 0 && s.memTotalMb > 0) {
            mem.setText(ctx.getString(R.string.widget_gb,
                    SystemStats.gbTenths(s.memUsedMb * 1024 * 1024),
                    SystemStats.gbTenths(s.memTotalMb * 1024 * 1024)));
            memBar.setProgress(percent(s.memUsedMb, s.memTotalMb));
        }
        if (s.storeUsedBytes >= 0 && s.storeTotalBytes > 0) {
            store.setText(ctx.getString(R.string.widget_gb,
                    SystemStats.gbTenths(s.storeUsedBytes), SystemStats.gbTenths(s.storeTotalBytes)));
            storeBar.setProgress(percent(s.storeUsedBytes, s.storeTotalBytes));
        }
    }

    private static int percent(long used, long total) {
        return total <= 0 ? 0 : (int) Math.max(0, Math.min(100, used * 100 / total));
    }

    private static String oneDecimal(double v) {
        return v >= 10 ? String.valueOf(Math.round(v)) : String.format(java.util.Locale.US, "%.1f", v);
    }

    /** Today's lunar date and the can chi year; cheap enough to call once a minute with the clock. */
    void setLunar() {
        if (lunarCard.getVisibility() != View.VISIBLE) return;
        LocalDate today = LocalDate.now();
        Lunar.Date d = Lunar.fromSolar(today, java.time.ZoneId.systemDefault());
        lunarYear.setText(Lunar.canChi(d.year));
        String date = d.day + "/" + d.month;
        lunarDate.setText(d.leap ? ctx.getString(R.string.widget_lunar_leap, date) : date);
        LocalDate christmas = LocalDate.of(today.getYear(), 12, 25);
        if (christmas.isBefore(today)) christmas = christmas.plusYears(1);
        lunarExtra.setText(ctx.getString(R.string.widget_christmas, ChronoUnit.DAYS.between(today, christmas)));
    }
}
