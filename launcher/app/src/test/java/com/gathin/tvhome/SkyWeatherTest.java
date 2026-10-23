package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkyWeatherTest {

    private static int luminance(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    @Test
    public void theLightDayIsLightAndTheLightNightIsDark() {
        Sky.Palette noon = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, true);
        assertTrue(luminance(noon.top) > 600);
        Sky.Palette night = Sky.at(60, 360, 1080, OpenMeteo.COND_CLEAR, true);
        assertTrue(luminance(night.top) < 200);
    }

    @Test
    public void theLightDayCarriesBlobsAndTheDarkOneDoesNot() {
        Sky.Palette noon = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, true);
        assertTrue(noon.blobA != 0 && noon.blobB != 0);
        Sky.Palette dark = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, false);
        assertEquals(0, dark.blobA);
        assertEquals(0, dark.blobB);
    }

    @Test
    public void rainGreysTheSkyAndAsksForStreaks() {
        Sky.Palette clear = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, true);
        Sky.Palette rain = Sky.at(780, 360, 1080, OpenMeteo.COND_RAIN, true);
        assertEquals(0, clear.precip);
        assertEquals(1, rain.precip);
        assertTrue(luminance(rain.top) < luminance(clear.top));
        assertTrue(rain.sun < clear.sun);
    }

    @Test
    public void snowPalesTheSkyAndAsksForFlakes() {
        Sky.Palette clear = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, true);
        Sky.Palette snow = Sky.at(780, 360, 1080, OpenMeteo.COND_SNOW, true);
        assertEquals(2, snow.precip);
        // Paler: the channels move apart less, towards a near white.
        int spreadClear = Math.abs(((clear.top >> 16) & 0xFF) - (clear.top & 0xFF));
        int spreadSnow = Math.abs(((snow.top >> 16) & 0xFF) - (snow.top & 0xFF));
        assertTrue(spreadSnow < spreadClear);
    }

    @Test
    public void thunderTakesTheLightOutOfBothSkins() {
        Sky.Palette darkClear = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, false);
        Sky.Palette darkStorm = Sky.at(780, 360, 1080, OpenMeteo.COND_THUNDER, false);
        assertTrue(luminance(darkStorm.top) < luminance(darkClear.top));
        assertTrue(darkStorm.sun <= darkClear.sun);
    }

    @Test
    public void fogHidesTheSunEntirely() {
        Sky.Palette fog = Sky.at(780, 360, 1080, OpenMeteo.COND_FOG, true);
        assertEquals(0, fog.sun);
        assertEquals(0, fog.precip);
    }

    @Test
    public void anUnknownConditionPaintsAsClear() {
        Sky.Palette clear = Sky.at(780, 360, 1080, OpenMeteo.COND_CLEAR, true);
        Sky.Palette unknown = Sky.at(780, 360, 1080, 42, true);
        assertEquals(clear.top, unknown.top);
        assertEquals(clear.sun, unknown.sun);
    }

    @Test
    public void theDarkPaletteKeepsItsHistoricalColours() {
        // The built-in skin must not drift when the weather feature is off.
        assertEquals(0xFF04060E, Sky.at(0).top);
        assertEquals(0xFF12203F, Sky.at(450).top);
    }
}
