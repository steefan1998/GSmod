package nl.gsmod.daily;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world config: {@code <world>/serverconfig/gsmod_daily-server.toml}. A file in {@code defaultconfigs/}
 * is copied into new worlds. The reward tables and missions themselves live in {@code <world>/gsmod_daily/}
 * (see {@link WorldJsonManager}).
 */
public final class DailyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DAILY_REWARDS_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> DAILY_TIME_ZONE;
    public static final ModConfigSpec.IntValue DAILY_RESET_HOUR;
    public static final ModConfigSpec.IntValue DAILY_STREAK_GRACE_DAYS;
    public static final ModConfigSpec.IntValue DAILY_WEEK_BONUS_EVERY;
    public static final ModConfigSpec.IntValue DAILY_COMEBACK_MIN_STREAK;
    public static final ModConfigSpec.BooleanValue DAILY_NOTIFY_ON_LOGIN;
    public static final ModConfigSpec.BooleanValue DAILY_MISSIONS_ENABLED;
    public static final ModConfigSpec.IntValue DAILY_MISSIONS_PER_DAY;

    static {
        BUILDER.comment("Daily login rewards and daily missions for this world.",
                "Reward tables and missions are JSON files in <world>/gsmod_daily/daily_rewards and <world>/gsmod_daily/daily_missions.").push("daily");
        DAILY_REWARDS_ENABLED = BUILDER.comment("Allow players to claim a daily reward with /daily")
                .define("rewardsEnabled", true);
        DAILY_TIME_ZONE = BUILDER.comment("Time zone the daily reset follows, e.g. \"UTC\", \"Europe/Amsterdam\", \"America/New_York\".",
                        "Summer and winter time (DST) are handled automatically for zones that use them.",
                        "Everyone on the server shares this one reset moment.")
                .define("timeZone", "UTC", DailyConfig::isValidZone);
        DAILY_RESET_HOUR = BUILDER.comment("Hour (0-23, in timeZone) at which a new day starts")
                .defineInRange("resetHour", 0, 0, 23);
        DAILY_STREAK_GRACE_DAYS = BUILDER.comment("Days a player may miss without losing their streak")
                .defineInRange("streakGraceDays", 0, 0, 30);
        DAILY_WEEK_BONUS_EVERY = BUILDER.comment("Give the week bonus (daily_rewards/bonus_week.json) on every Nth day in a row; 0 = off")
                .defineInRange("weekBonusEvery", 7, 0, 365);
        DAILY_COMEBACK_MIN_STREAK = BUILDER.comment("Give the comeback reward (daily_rewards/bonus_comeback.json) when a streak of at least this many days is lost; 0 = off")
                .defineInRange("comebackMinStreak", 3, 0, 365);
        DAILY_NOTIFY_ON_LOGIN = BUILDER.comment("Tell players on login when a reward is waiting")
                .define("notifyOnLogin", true);
        DAILY_MISSIONS_ENABLED = BUILDER.comment("Give players daily missions")
                .define("missionsEnabled", true);
        DAILY_MISSIONS_PER_DAY = BUILDER.comment("How many missions each player gets per day")
                .defineInRange("missionsPerDay", 3, 1, 20);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean isValidZone(Object value) {
        try {
            return value instanceof String s && ZoneId.of(s) != null;
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** The configured reset time zone; UTC if the config isn't loaded or the value is invalid. */
    public static ZoneId zone() {
        if (!SPEC.isLoaded()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(DAILY_TIME_ZONE.get());
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }

    public static int resetHour() {
        return SPEC.isLoaded() ? DAILY_RESET_HOUR.get() : 0;
    }

    private DailyConfig() {}
}
