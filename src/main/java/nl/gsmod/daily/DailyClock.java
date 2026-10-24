package nl.gsmod.daily;

import java.time.Instant;

/**
 * Reward days are counted in UTC, so the reset happens at the same moment for every player
 * regardless of their own or the server's time zone. {@code resetHourUtc} shifts the reset moment.
 */
public final class DailyClock {
    private static final long DAY = 86_400L;

    /** Number of whole reward days since the epoch. */
    public static long day(Instant now, int resetHourUtc) {
        return Math.floorDiv(now.getEpochSecond() - resetHourUtc * 3600L, DAY);
    }

    public static long secondsUntilReset(Instant now, int resetHourUtc) {
        long shifted = now.getEpochSecond() - resetHourUtc * 3600L;
        return DAY - Math.floorMod(shifted, DAY);
    }

    public static String formatDuration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60;
        return h > 0 ? h + "h " + m + "m" : m + "m " + (seconds % 60) + "s";
    }

    private DailyClock() {}
}
