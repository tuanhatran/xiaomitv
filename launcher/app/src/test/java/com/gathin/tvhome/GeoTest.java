package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GeoTest {

    @Test
    public void readsIpWho() {
        Geo.Place p = Geo.parseIpWho("{\"ip\":\"1.53.120.4\",\"success\":true,\"city\":\"Da Nang\","
                + "\"latitude\":16.0678,\"longitude\":108.2208}");
        assertEquals(16.0678, p.lat, 0.0001);
        assertEquals(108.2208, p.lon, 0.0001);
        assertEquals("Da Nang", p.city);
    }

    @Test
    public void ipWhoFailureIsNotAPlace() {
        assertNull(Geo.parseIpWho("{\"ip\":\"127.0.0.1\",\"success\":false,\"message\":\"Reserved range\"}"));
    }

    @Test
    public void readsGeoJsWhichWritesCoordinatesAsText() {
        Geo.Place p = Geo.parseGeoJs("{\"ip\":\"1.53.120.4\",\"latitude\":\"16.0678\","
                + "\"longitude\":\"108.2208\",\"city\":\"Da Nang\"}");
        assertEquals(16.0678, p.lat, 0.0001);
        assertEquals(108.2208, p.lon, 0.0001);
        assertEquals("Da Nang", p.city);
    }

    @Test
    public void rejectsPlacesThatAreNotOnEarth() {
        assertNull(Geo.parseIpWho("{\"success\":true,\"latitude\":95,\"longitude\":10}"));
        assertNull(Geo.parseGeoJs("{\"latitude\":\"0.0\",\"longitude\":\"0\"}"));
        assertNull(Geo.parseIpWho("{\"success\":true,\"city\":\"Nowhere\"}"));
        assertNull(Geo.parseGeoJs("not json"));
    }

    @Test
    public void aMissingCityIsAnEmptyNameNotNull() {
        Geo.Place p = Geo.parseIpWho("{\"success\":true,\"latitude\":16,\"longitude\":108}");
        assertEquals("", p.city);
    }
}
