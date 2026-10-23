package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OpenMeteoTest {

    private static final String FORECAST = "{"
            + "\"current\":{\"time\":\"2026-10-21T14:15\",\"interval\":900,\"temperature_2m\":28.4,"
            + "\"is_day\":1,\"weather_code\":63},"
            + "\"daily\":{\"time\":[\"2026-10-21\"],\"temperature_2m_max\":[29.1],\"temperature_2m_min\":[24.8]}"
            + "}";

    private static final String AIR = "{\"current\":{\"time\":\"2026-10-21T14:00\",\"us_aqi\":38,\"pm2_5\":4.7}}";

    @Test
    public void readsTheForecast() {
        OpenMeteo.Forecast f = OpenMeteo.parseForecast(FORECAST);
        assertEquals(28.4, f.temp, 0.001);
        assertEquals(29.1, f.max, 0.001);
        assertEquals(24.8, f.min, 0.001);
        assertEquals(63, f.code);
        assertTrue(f.day);
    }

    @Test
    public void readsTheAirQuality() {
        OpenMeteo.Air a = OpenMeteo.parseAir(AIR);
        assertEquals(38, a.aqi);
        assertEquals(4.7, a.pm25, 0.001);
    }

    @Test
    public void rejectsAnswersWithoutTheFieldsTheWidgetShows() {
        assertNull(OpenMeteo.parseForecast("{\"current\":{\"temperature_2m\":20}}"));
        assertNull(OpenMeteo.parseForecast("{\"daily\":{}}"));
        assertNull(OpenMeteo.parseForecast("not json"));
        assertNull(OpenMeteo.parseAir("{\"current\":{\"pm2_5\":3}}"));
        assertNull(OpenMeteo.parseAir("{}"));
    }

    @Test
    public void airWithoutPm25StillCounts() {
        OpenMeteo.Air a = OpenMeteo.parseAir("{\"current\":{\"us_aqi\":12}}");
        assertEquals(12, a.aqi);
        assertEquals(-1, a.pm25, 0);
    }

    @Test
    public void wmoCodesMapToSkyConditions() {
        assertEquals(OpenMeteo.COND_CLEAR, OpenMeteo.condition(0));
        assertEquals(OpenMeteo.COND_PARTLY, OpenMeteo.condition(1));
        assertEquals(OpenMeteo.COND_PARTLY, OpenMeteo.condition(2));
        assertEquals(OpenMeteo.COND_CLOUDY, OpenMeteo.condition(3));
        assertEquals(OpenMeteo.COND_FOG, OpenMeteo.condition(45));
        assertEquals(OpenMeteo.COND_RAIN, OpenMeteo.condition(51));
        assertEquals(OpenMeteo.COND_RAIN, OpenMeteo.condition(63));
        assertEquals(OpenMeteo.COND_RAIN, OpenMeteo.condition(80));
        assertEquals(OpenMeteo.COND_SNOW, OpenMeteo.condition(71));
        assertEquals(OpenMeteo.COND_SNOW, OpenMeteo.condition(86));
        assertEquals(OpenMeteo.COND_THUNDER, OpenMeteo.condition(95));
        assertEquals(OpenMeteo.COND_THUNDER, OpenMeteo.condition(99));
    }

    @Test
    public void urlsCarryThePlaceAndTheFieldsAskedFor() {
        String url = OpenMeteo.forecastUrl(16.067, 108.225);
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=16.067&longitude=108.225"));
        assertTrue(url.contains("current=temperature_2m,is_day,weather_code"));
        assertTrue(url.contains("daily=temperature_2m_max,temperature_2m_min"));
        String air = OpenMeteo.airUrl(-1.5, 30.25);
        assertTrue(air.contains("latitude=-1.500"));
        assertTrue(air.contains("current=us_aqi,pm2_5"));
    }
}
