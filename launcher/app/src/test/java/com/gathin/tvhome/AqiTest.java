package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AqiTest {

    @Test
    public void usBandsNameTheLevels() {
        assertEquals(Aqi.LEVEL_GOOD, Aqi.level(0));
        assertEquals(Aqi.LEVEL_GOOD, Aqi.level(38));
        assertEquals(Aqi.LEVEL_GOOD, Aqi.level(50));
        assertEquals(Aqi.LEVEL_MODERATE, Aqi.level(51));
        assertEquals(Aqi.LEVEL_MODERATE, Aqi.level(100));
        assertEquals(Aqi.LEVEL_POOR, Aqi.level(101));
        assertEquals(Aqi.LEVEL_POOR, Aqi.level(200));
        assertEquals(Aqi.LEVEL_VERY_POOR, Aqi.level(201));
    }

    @Test
    public void cleanerAirKeepsADeeperGreen() {
        int good = Aqi.colour(Aqi.LEVEL_GOOD);
        int poor = Aqi.colour(Aqi.LEVEL_POOR);
        assertTrue(green(good) > green(poor));
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }
}
