package com.gathin.tvhome;

import org.json.JSONObject;

/**
 * Where the TV is, from its public address: ipwho.is first, geojs.io when that answers nothing
 * usable. Both are keyless and answer the city with the coordinates, which is all the weather
 * widget and the sky need. Parsing is pure so it runs in JVM tests.
 */
final class Geo {

    static final String IPWHO_URL = "https://ipwho.is/";
    static final String GEOJS_URL = "https://get.geojs.io/v1/ip/geo.json";

    /** A place good enough to point a forecast at. */
    static final class Place {
        final double lat;
        final double lon;
        /** City name as the service writes it; empty when it gave none. */
        final String city;

        Place(double lat, double lon, String city) {
            this.lat = lat;
            this.lon = lon;
            this.city = city;
        }
    }

    private Geo() {
    }

    /**
     * An ipwho.is answer: numbers for the coordinates and {@code "success": false} when the address
     * is private or the service is unhappy. Null when the place cannot be used.
     */
    static Place parseIpWho(String json) {
        try {
            JSONObject o = new JSONObject(json);
            if (o.has("success") && !o.optBoolean("success", false)) return null;
            double lat = o.optDouble("latitude", Double.NaN);
            double lon = o.optDouble("longitude", Double.NaN);
            return place(lat, lon, o.optString("city", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A geojs.io answer: the coordinates come back as decimal text in strings. Null when they do
     * not parse into a place on Earth.
     */
    static Place parseGeoJs(String json) {
        try {
            JSONObject o = new JSONObject(json);
            double lat = number(o.opt("latitude"));
            double lon = number(o.opt("longitude"));
            return place(lat, lon, o.optString("city", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Place place(double lat, double lon, String city) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return null;
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
        // A place in the open ocean or on the null island marker is a service failure, not a town.
        if (lat == 0 && lon == 0) return null;
        String name = city == null ? "" : city.trim();
        return new Place(lat, lon, name);
    }

    /** A JSON number or its decimal text, as geojs.io writes coordinates. */
    private static double number(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try {
                return Double.parseDouble(((String) v).trim());
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }
}
