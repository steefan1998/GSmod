package nl.gsmod.daily;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import nl.gsmod.ServerConfig;
import nl.gsmod.GSMod;

/**
 * Server → client snapshot for the Daily screen. Display only: the client never decides anything,
 * claiming still goes through the server-side {@code /daily claim} command.
 */
public record DailyStatusPayload(boolean rewardsEnabled, boolean canClaim, int streak, int nextStreak, Component nextTier, long secondsUntilReset,
        List<Line> missions) implements CustomPacketPayload {

    public record Line(Component title, long progress, long amount, boolean completed) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, Line::title,
                ByteBufCodecs.VAR_LONG, Line::progress,
                ByteBufCodecs.VAR_LONG, Line::amount,
                ByteBufCodecs.BOOL, Line::completed,
                Line::new);
    }

    public static final Type<DailyStatusPayload> TYPE = new Type<>(GSMod.id("daily_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DailyStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DailyStatusPayload::rewardsEnabled,
            ByteBufCodecs.BOOL, DailyStatusPayload::canClaim,
            ByteBufCodecs.VAR_INT, DailyStatusPayload::streak,
            ByteBufCodecs.VAR_INT, DailyStatusPayload::nextStreak,
            ComponentSerialization.STREAM_CODEC, DailyStatusPayload::nextTier,
            ByteBufCodecs.VAR_LONG, DailyStatusPayload::secondsUntilReset,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list(64)), DailyStatusPayload::missions,
            DailyStatusPayload::new);

    public static DailyStatusPayload of(ServerPlayer player, Instant now) {
        long today = DailyRewards.today(now);
        DailyRewardData data = DailyRewards.data(player);
        int grace = ServerConfig.DAILY_STREAK_GRACE_DAYS.get();
        // Shown streak drops to 0 once it can no longer be continued.
        int streak = data.canClaim(today) && data.nextStreak(today, grace) == 1 ? 0 : data.streak();

        List<Line> lines = new ArrayList<>();
        if (DailyMissions.enabled()) {
            Map<Identifier, Mission> all = MissionManager.INSTANCE.all();
            for (DailyMissionData.Entry entry : DailyMissions.current(player, now).missions()) {
                Mission mission = all.get(entry.mission());
                if (mission != null) {
                    boolean minutes = mission.objective() == MissionObjective.PLAY_TIME;
                    lines.add(new Line(mission.displayName(),
                            minutes ? entry.progress() / 60 : entry.progress(),
                            minutes ? Math.max(1, mission.amount() / 60) : mission.amount(),
                            entry.completed()));
                }
            }
        }
        return new DailyStatusPayload(ServerConfig.DAILY_REWARDS_ENABLED.get(), DailyRewards.canClaim(player, now), streak,
                data.nextStreak(today, grace), nextTier(data.nextStreak(today, grace)), DailyClock.secondsUntilReset(now, ServerConfig.DAILY_RESET_HOUR_UTC.get()), List.copyOf(lines));
    }

    private static Component nextTier(int streak) {
        RewardTable table = RewardTableManager.INSTANCE.forStreak(streak);
        return table == null ? Component.empty() : table.tierName();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
