package com.gathin.tvhome;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LunarTest {

    private static Lunar.Date lunar(int y, int m, int d) {
        return Lunar.fromSolar(LocalDate.of(y, m, d), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    public void tetIsTheFirstDayOfTheLunarYear() {
        Lunar.Date t2024 = lunar(2024, 2, 10);
        assertEquals(1, t2024.day);
        assertEquals(1, t2024.month);
        assertEquals(2024, t2024.year);
        Lunar.Date t2025 = lunar(2025, 1, 29);
        assertEquals(1, t2025.day);
        assertEquals(1, t2025.month);
        assertEquals(2025, t2025.year);
        Lunar.Date t2026 = lunar(2026, 2, 17);
        assertEquals(1, t2026.day);
        assertEquals(1, t2026.month);
        assertEquals(2026, t2026.year);
    }

    @Test
    public void theDayBeforeTetIsTheLastDayOfTheOldYear() {
        Lunar.Date eve = lunar(2026, 2, 16);
        assertEquals(2025, eve.year);
        assertEquals(12, eve.month);
        assertTrue(eve.day == 29 || eve.day == 30);
        assertFalse(eve.leap);
    }

    @Test
    public void autumn2026MatchesTheWidget() {
        // The reference design shows 12/9 of the year Bính Ngọ for 21 October 2026.
        Lunar.Date d = lunar(2026, 10, 21);
        assertEquals(12, d.day);
        assertEquals(9, d.month);
        assertEquals(2026, d.year);
        assertEquals("Bính Ngọ", Lunar.canChi(d.year));
    }

    @Test
    public void leapMonthIsFlaggedOnlyInsideItself() {
        // 2025 carries a leap month 6: it starts at the end of July.
        Lunar.Date inLeap = lunar(2025, 7, 25);
        assertEquals(6, inLeap.month);
        assertTrue(inLeap.leap);
        Lunar.Date before = lunar(2025, 7, 23);
        assertEquals(6, before.month);
        assertFalse(before.leap);
    }

    @Test
    public void canChiNamesTheCycle() {
        assertEquals("Giáp Thìn", Lunar.canChi(2024));
        assertEquals("Ất Tỵ", Lunar.canChi(2025));
        assertEquals("Bính Ngọ", Lunar.canChi(2026));
        assertEquals("Canh Tuất", Lunar.canChi(2030));
    }

    @Test
    public void daysRunOneToThirtyInsideAMonth() {
        LocalDate day = LocalDate.of(2026, 3, 1);
        Lunar.Date prev = Lunar.fromSolar(day, ZoneId.of("Asia/Ho_Chi_Minh"));
        for (int i = 0; i < 60; i++) {
            day = day.plusDays(1);
            Lunar.Date d = Lunar.fromSolar(day, ZoneId.of("Asia/Ho_Chi_Minh"));
            if (d.month == prev.month && d.leap == prev.leap && d.year == prev.year) {
                assertEquals(prev.day + 1, d.day);
            } else {
                assertEquals(1, d.day);
            }
            prev = d;
        }
    }
}
