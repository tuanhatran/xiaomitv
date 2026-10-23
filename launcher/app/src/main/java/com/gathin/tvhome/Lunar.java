package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The Vietnamese lunisolar calendar: the lunar day and month of a solar date, and the can chi
 * (stem and branch) name of its lunar year, "Bính Ngọ" for the year that starts in February 2026.
 *
 * Pure maths, no Android types: the astronomical part is the published algorithm of Hồ Ngọc Đức
 * (new moons and solar longitudes from Meeus, "Astronomical Algorithms"), evaluated in the time
 * zone the caller passes, so the same new moon falls on the local day the TV lives in. The can
 * chi names are the Vietnamese reading of the shared 60-name cycle.
 */
final class Lunar {

    /** One lunar date: day and month of the lunar year, and whether the month is a leap one. */
    static final class Date {
        final int day;
        final int month;
        final int year;
        final boolean leap;

        Date(int day, int month, int year, boolean leap) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
        }
    }

    /** The ten can (stems), in order. */
    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    /** The twelve chi (branches), in order. */
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};

    /** Julian day number of the new moon of 1 January 1900, the epoch the new moon index counts from. */
    private static final double J1900 = 2415021.076998695;
    /** Mean days between two new moons. */
    private static final double CYCLE = 29.530588853;

    private Lunar() {
    }

    /** The lunar date of a solar day in the zone's offset at its noon. */
    static Date fromSolar(LocalDate solar, ZoneId zone) {
        int offsetHours = zone.getRules().getOffset(solar.atTime(12, 0)).getTotalSeconds() / 3600;
        int dayNumber = jdFromDate(solar.getDayOfMonth(), solar.getMonthValue(), solar.getYear());
        int k = (int) Math.floor((dayNumber - J1900) / CYCLE);
        int monthStart = newMoonDay(k + 1, offsetHours);
        if (monthStart > dayNumber) monthStart = newMoonDay(k, offsetHours);
        int a11 = lunarMonth11(solar.getYear(), offsetHours);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = solar.getYear();
            a11 = lunarMonth11(solar.getYear() - 1, offsetHours);
        } else {
            lunarYear = solar.getYear() + 1;
            b11 = lunarMonth11(solar.getYear() + 1, offsetHours);
        }
        int diff = (monthStart - a11) / 29;
        int lunarMonth = diff + 11;
        boolean leap = false;
        if (b11 - a11 > 365) {
            int leapOffset = leapMonthOffset(a11, offsetHours);
            if (diff >= leapOffset) {
                lunarMonth = diff + 10;
                leap = diff == leapOffset;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
        return new Date(dayNumber - monthStart + 1, lunarMonth, lunarYear, leap);
    }

    /** The can chi name of a lunar year: "Bính Ngọ" for 2026. */
    static String canChi(int lunarYear) {
        int stem = Math.floorMod(lunarYear - 4, 10);
        int branch = Math.floorMod(lunarYear - 4, 12);
        return CAN[stem] + " " + CHI[branch];
    }

    // ---- astronomy -------------------------------------------------------------------------------

    /** Integral Julian day number of a Gregorian date (Tøndering's calendar formulae). */
    static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        return jd;
    }

    /** The k-th new moon after the one of 1 January 1900, as a fractional Julian day in UT. */
    static double newMoon(int k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double dr = Math.PI / 180;
        double jd = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
        jd += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
        double mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
        double c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * dr * m);
        c1 = c1 - 0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(dr * 2 * mpr);
        c1 = c1 - 0.0004 * Math.sin(dr * 3 * mpr);
        c1 = c1 + 0.0104 * Math.sin(dr * 2 * f) - 0.0051 * Math.sin(dr * (m + mpr));
        c1 = c1 - 0.0074 * Math.sin(dr * (m - mpr)) + 0.0004 * Math.sin(dr * (2 * f + m));
        c1 = c1 - 0.0004 * Math.sin(dr * (2 * f - m)) - 0.0006 * Math.sin(dr * (2 * f + mpr));
        c1 = c1 + 0.0010 * Math.sin(dr * (2 * f - mpr)) + 0.0005 * Math.sin(dr * (2 * mpr + m));
        double delta = t < -11
                ? 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
                : -0.000278 + 0.000265 * t + 0.000262 * t2;
        return jd + c1 - delta;
    }

    /** True ecliptic longitude of the sun at a fractional Julian day, in radians, 0 to 2 pi. */
    static double sunLongitude(double jdn) {
        double t = (jdn - 2451545.0) / 36525;
        double t2 = t * t;
        double dr = Math.PI / 180;
        double m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
        double l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
        double dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m);
        dl = dl + (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.000290 * Math.sin(dr * 3 * m);
        double l = (l0 + dl) * dr;
        return l - 2 * Math.PI * Math.floor(l / (2 * Math.PI));
    }

    /** Which of the twelve solar terms the sun is in at local midnight of a Julian day number, 0 to 11. */
    static int sunTerm(int dayNumber, int offsetHours) {
        return (int) Math.floor(sunLongitude(dayNumber - 0.5 - offsetHours / 24.0) / Math.PI * 6);
    }

    /** The local day the k-th new moon falls on. */
    static int newMoonDay(int k, int offsetHours) {
        return (int) Math.floor(newMoon(k) + 0.5 + offsetHours / 24.0);
    }

    /** The local day starting lunar month 11 (the one holding the winter solstice) of a solar year. */
    static int lunarMonth11(int yy, int offsetHours) {
        int k = (int) Math.floor((jdFromDate(31, 12, yy) - 2415021) / CYCLE);
        int nm = newMoonDay(k, offsetHours);
        if (sunTerm(nm, offsetHours) >= 9) nm = newMoonDay(k - 1, offsetHours);
        return nm;
    }

    /** How many months after month 11 the leap month of the cycle starting at a11 falls. */
    static int leapMonthOffset(int a11, int offsetHours) {
        int k = (int) Math.floor((a11 - J1900) / CYCLE + 0.5);
        int i = 1;
        int last = 0;
        int arc = sunTerm(newMoonDay(k + i, offsetHours), offsetHours);
        do {
            last = arc;
            i++;
            arc = sunTerm(newMoonDay(k + i, offsetHours), offsetHours);
        } while (arc != last && i < 14);
        return i - 1;
    }
}
