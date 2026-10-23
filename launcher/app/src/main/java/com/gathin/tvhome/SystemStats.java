package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * What the machine is doing right now: CPU busy percent, memory and storage used against their
 * totals. The system widget and the optional stats line under the clock share one reader, so
 * /proc is opened once per sample, not once per widget.
 *
 * The parsers are pure (a /proc line in, a number out) and unit tested; the readers that open
 * files and ask Android for totals live on the instance, which keeps the sampling state of the
 * CPU counter between calls.
 */
final class SystemStats {

    /** One sample; -1 where a source could not be read yet or at all. */
    static final class Sample {
        /** Busy percent 0 to 100, -1 until a second sample, -2 where the kernel hides the counters. */
        final int cpu;
        final long memUsedMb;
        final long memTotalMb;
        final long storeUsedBytes;
        final long storeTotalBytes;
        /** Free memory in MB, for the stats line under the clock. */
        final long memFreeMb;
        /** Free storage in bytes, for the stats line under the clock. */
        final long storeFreeBytes;

        Sample(int cpu, long memUsedMb, long memTotalMb, long memFreeMb, long storeUsedBytes, long storeTotalBytes,
               long storeFreeBytes) {
            this.cpu = cpu;
            this.memUsedMb = memUsedMb;
            this.memTotalMb = memTotalMb;
            this.memFreeMb = memFreeMb;
            this.storeUsedBytes = storeUsedBytes;
            this.storeTotalBytes = storeTotalBytes;
            this.storeFreeBytes = storeFreeBytes;
        }
    }

    /** -2: this source cannot be read (Android 9 hides /proc/stat from apps on many TVs). */
    static final int CPU_UNREADABLE = -2;

    private long cpuTotalPrev = -1, cpuIdlePrev = -1;
    private boolean procStatDead;
    private long idlePrev = -1, wallPrev = -1;

    // ---- pure parsers ----------------------------------------------------------------------------

    /**
     * Busy percent between two samples of the first /proc/stat line. {@code state} carries
     * {total, idle} of the previous sample, both -1 before the first: the first call stores and
     * returns -1, since a percentage needs two samples. Malformed lines return {@link #CPU_UNREADABLE}.
     */
    static int cpuFromProcStat(String line, long[] state) {
        if (line == null || !line.startsWith("cpu")) return CPU_UNREADABLE;
        String[] t = line.trim().split("\\s+");
        if (t.length < 5) return CPU_UNREADABLE;
        try {
            long user = Long.parseLong(t[1]), nice = Long.parseLong(t[2]), sys = Long.parseLong(t[3]);
            long idle = Long.parseLong(t[4]);
            long iowait = t.length > 5 ? Long.parseLong(t[5]) : 0;
            long irq = t.length > 6 ? Long.parseLong(t[6]) : 0;
            long soft = t.length > 7 ? Long.parseLong(t[7]) : 0;
            long total = user + nice + sys + idle + iowait + irq + soft;
            long idleAll = idle + iowait;
            if (state[0] < 0) {
                state[0] = total;
                state[1] = idleAll;
                return -1;
            }
            long dt = total - state[0], di = idleAll - state[1];
            state[0] = total;
            state[1] = idleAll;
            if (dt <= 0) return -1;
            return Math.max(0, Math.min(100, (int) Math.round((dt - di) * 100.0 / dt)));
        } catch (NumberFormatException e) {
            return CPU_UNREADABLE;
        }
    }

    /** The MemAvailable figure of one /proc/meminfo line, in KB; -1 when the line is not it. */
    static long memAvailableKb(String line) {
        if (line == null || !line.startsWith("MemAvailable:")) return -1;
        String[] t = line.split("\\s+");
        try {
            return t.length >= 2 ? Long.parseLong(t[1]) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** "3.1 GB", one decimal under ten and none above, empty for nothing. */
    static String formatGb(long bytes) {
        if (bytes <= 0) return "";
        double gb = bytes / (1024.0 * 1024 * 1024);
        return gb >= 10 ? String.format(Locale.US, "%.0f GB", gb)
                : String.format(Locale.US, "%.1f GB", gb);
    }

    /** "3.1", always one decimal: the used/total figures of the system widget. */
    static String gbTenths(long bytes) {
        return String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024 * 1024));
    }

    // ---- readers ---------------------------------------------------------------------------------

    /** Busy percent, /proc/stat first and the cpuidle counters where the kernel hides it. */
    int cpuPercent() {
        if (!procStatDead) {
            long[] state = {cpuTotalPrev, cpuIdlePrev};
            int fromStat = cpuFromProcStat(readFirstLine("/proc/stat"), state);
            cpuTotalPrev = state[0];
            cpuIdlePrev = state[1];
            if (fromStat != CPU_UNREADABLE) return fromStat;
            procStatDead = true;
            idlePrev = -1;
        }
        return cpuFromIdle();
    }

    /** Busy percent from cpuidle time (microseconds) across the cores. -1 until the second sample. */
    private int cpuFromIdle() {
        long idle = 0;
        int cores = 0;
        for (int i = 0; i < 8; i++) {
            boolean any = false;
            for (int state = 0; state < 8; state++) {
                String t = readFirstLine("/sys/devices/system/cpu/cpu" + i
                        + "/cpuidle/state" + state + "/time");
                if (t == null) break;
                try {
                    idle += Long.parseLong(t.trim());
                    any = true;
                } catch (NumberFormatException e) {
                    break;
                }
            }
            if (!any) break;
            cores++;
        }
        if (cores == 0) return -1;
        long wall = SystemClock.elapsedRealtime() * 1000L;
        if (idlePrev < 0) {
            idlePrev = idle;
            wallPrev = wall;
            return -1;
        }
        long di = idle - idlePrev;
        long dw = (wall - wallPrev) * cores;
        idlePrev = idle;
        wallPrev = wall;
        if (dw <= 0) return -1;
        return Math.max(0, Math.min(100, (int) Math.round((dw - di) * 100.0 / dw)));
    }

    /** One sample of everything; the CPU figure may be -1, the caller samples again shortly after. */
    Sample sample(Context ctx) {
        int cpu = cpuPercent();
        long memFreeMb = memAvailableMb();
        long memTotalMb = -1;
        Object service = ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (service instanceof ActivityManager) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            ((ActivityManager) service).getMemoryInfo(info);
            if (info.totalMem > 0) memTotalMb = info.totalMem / (1024 * 1024);
        }
        long memUsedMb = memFreeMb >= 0 && memTotalMb > 0 ? Math.max(0, memTotalMb - memFreeMb) : -1;
        long storeFree = -1, storeTotal = -1;
        try {
            java.io.File data = Environment.getDataDirectory();
            storeFree = data.getFreeSpace();
            storeTotal = data.getTotalSpace();
        } catch (Exception e) {
            storeFree = -1;
        }
        long storeUsed = storeFree >= 0 && storeTotal > 0 ? Math.max(0, storeTotal - storeFree) : -1;
        return new Sample(cpu, memUsedMb, memTotalMb, memFreeMb, storeUsed, storeTotal, storeFree);
    }

    private static long memAvailableMb() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                long kb = memAvailableKb(line);
                if (kb >= 0) return kb / 1024;
            }
        } catch (IOException ignored) {
        }
        return -1;
    }

    private static String readFirstLine(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            return r.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
