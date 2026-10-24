package nl.gsmod.daily;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Reward days follow the wall clock of one configured time zone, so the reset happens at the same moment
 * for every player. Summer/winter time is handled by {@link ZoneId}: a day can be 23 or 25 hours long, but
 * the reset always lands on {@code resetHour} local time.
 */
public final class DailyClock {
    /** Reward day number: the local date in {@code zone}, counting a new day from {@code resetHour}. */
    public static long day(Instant now, ZoneId zone, int resetHour) {
        return now.atZone(zone).toLocalDateTime().minusHours(resetHour).toLocalDate().toEpochDay();
    }

    /** The moment reward day {@code day} starts. A reset hour skipped by DST moves to the first valid time. */
    public static Instant startOf(long day, ZoneId zone, int resetHour) {
        return ZonedDateTime.of(LocalDate.ofEpochDay(day), LocalTime.of(resetHour, 0), zone).toInstant();
    }

    public static long secondsUntilReset(Instant now, ZoneId zone, int resetHour) {
        Instant next = startOf(day(now, zone, resetHour) + 1, zone, resetHour);
        return Math.max(0, Duration.between(now, next).getSeconds());
    }

    /** Using the per-world config. */
    public static long secondsUntilReset(Instant now) {
        return secondsUntilReset(now, DailyConfig.zone(), DailyConfig.resetHour());
    }

    public static String formatDuration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60;
        return h > 0 ? h + "h " + m + "m" : m + "m " + (seconds % 60) + "s";
    }

    private DailyClock() {}
}
