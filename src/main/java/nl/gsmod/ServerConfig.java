package nl.gsmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world config: {@code <world>/serverconfig/gsmod-server.toml}. A file in {@code defaultconfigs/}
 * is copied into new worlds. The reward tables and missions themselves live in {@code <world>/gsmod/}
 * (see {@link nl.gsmod.daily.DailyWorldFiles}).
 */
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DAILY_REWARDS_ENABLED;
    public static final ModConfigSpec.IntValue DAILY_RESET_HOUR_UTC;
    public static final ModConfigSpec.IntValue DAILY_STREAK_GRACE_DAYS;
    public static final ModConfigSpec.BooleanValue DAILY_NOTIFY_ON_LOGIN;
    public static final ModConfigSpec.BooleanValue DAILY_MISSIONS_ENABLED;
    public static final ModConfigSpec.IntValue DAILY_MISSIONS_PER_DAY;

    static {
        BUILDER.comment("Daily login rewards and daily missions for this world.",
                "Reward tables and missions are JSON files in <world>/gsmod/daily_rewards and <world>/gsmod/daily_missions.").push("daily");
        DAILY_REWARDS_ENABLED = BUILDER.comment("Allow players to claim a daily reward with /daily")
                .define("rewardsEnabled", true);
        DAILY_RESET_HOUR_UTC = BUILDER.comment("Hour (UTC) at which a new day starts. UTC is used so the reset is the same for everyone, whatever their time zone.")
                .defineInRange("resetHourUtc", 0, 0, 23);
        DAILY_STREAK_GRACE_DAYS = BUILDER.comment("Days a player may miss without losing their streak")
                .defineInRange("streakGraceDays", 0, 0, 30);
        DAILY_NOTIFY_ON_LOGIN = BUILDER.comment("Tell players on login when a reward is waiting")
                .define("notifyOnLogin", true);
        DAILY_MISSIONS_ENABLED = BUILDER.comment("Give players daily missions")
                .define("missionsEnabled", true);
        DAILY_MISSIONS_PER_DAY = BUILDER.comment("How many missions each player gets per day")
                .defineInRange("missionsPerDay", 3, 1, 20);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {}
}
