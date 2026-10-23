package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemStatsTest {

    @Test
    public void theFirstSampleStoresAndAsksForAnother() {
        long[] state = {-1, -1};
        assertEquals(-1, SystemStats.cpuFromProcStat("cpu  100 0 50 800 10 0 5 0 0 0", state));
        assertEquals(965, state[0]);
        assertEquals(810, state[1]);
    }

    @Test
    public void busyPercentIsTheDeltaWithoutIdle() {
        long[] state = {-1, -1};
        SystemStats.cpuFromProcStat("cpu  100 0 50 800 0 0 0 0 0 0", state);
        // total grows by 200, idle by 150: a quarter of the time was busy.
        assertEquals(25, SystemStats.cpuFromProcStat("cpu  125 0 75 950 0 0 0 0 0 0", state));
    }

    @Test
    public void aStillClockIsNotAPercentage() {
        long[] state = {-1, -1};
        SystemStats.cpuFromProcStat("cpu  100 0 50 800 0 0 0 0 0 0", state);
        assertEquals(-1, SystemStats.cpuFromProcStat("cpu  100 0 50 800 0 0 0 0 0 0", state));
    }

    @Test
    public void garbageLinesAreUnreadableNotZero() {
        long[] state = {-1, -1};
        assertEquals(SystemStats.CPU_UNREADABLE, SystemStats.cpuFromProcStat(null, state));
        assertEquals(SystemStats.CPU_UNREADABLE, SystemStats.cpuFromProcStat("cpu  x y", state));
        assertEquals(SystemStats.CPU_UNREADABLE, SystemStats.cpuFromProcStat("intr 1 2 3", state));
    }

    @Test
    public void meminfoLineGivesKilobytes() {
        assertEquals(430123, SystemStats.memAvailableKb("MemAvailable:    430123 kB"));
        assertEquals(-1, SystemStats.memAvailableKb("MemTotal:        2048000 kB"));
        assertEquals(-1, SystemStats.memAvailableKb(null));
    }

    @Test
    public void gigabytesRoundLikeTheReferenceDesign() {
        assertEquals("3.1 GB", SystemStats.formatGb((long) (3.14 * 1024 * 1024 * 1024)));
        assertEquals("12 GB", SystemStats.formatGb((long) (12.4 * 1024 * 1024 * 1024)));
        assertEquals("", SystemStats.formatGb(0));
        assertEquals("1.4", SystemStats.gbTenths((long) (1.44 * 1024 * 1024 * 1024)));
        assertEquals("9.2", SystemStats.gbTenths((long) (9.24 * 1024 * 1024 * 1024)));
    }
}
