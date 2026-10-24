package nl.gsmod.daily;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.attachment.AttachmentType;
import nl.gsmod.ServerConfig;

/**
 * Daily missions: every player gets {@code missionsPerDay} missions per reward day, picked from the
 * datapack missions by weight. The pick is seeded by player and day, so it is the same all day and the
 * same after a relog. Progress is tracked on the server only; a finished mission pays out immediately.
 */
public final class DailyMissions {
    public static final Supplier<AttachmentType<DailyMissionData>> DATA = DailyRewards.MISSION_DATA;

    public static boolean enabled() {
        return ServerConfig.SPEC.isLoaded() && ServerConfig.DAILY_MISSIONS_ENABLED.get();
    }

    /** Current missions, re-rolled first if a new day started or the datapack changed. */
    public static DailyMissionData current(ServerPlayer player, Instant now) {
        long today = DailyRewards.today(now);
        Map<Identifier, Mission> all = MissionManager.INSTANCE.all();
        DailyMissionData data = player.getData(DATA);
        boolean stale = data.day() != today || data.missions().stream().anyMatch(e -> !all.containsKey(e.mission()));
        if (stale) {
            data = roll(player, today, 0);
            player.setData(DATA, data);
        }
        return data;
    }

    /** Day of the reward cycle the player is on today: the day they claim (or claimed) today. */
    public static int cycleDay(ServerPlayer player, long today) {
        int cycle = RewardTableManager.INSTANCE.cycleLength();
        if (cycle == 0) {
            return 1;
        }
        DailyRewardData data = DailyRewards.data(player);
        int streak = data.lastClaimDay() == today ? data.streak() : data.nextStreak(today, ServerConfig.DAILY_STREAK_GRACE_DAYS.get());
        return (Math.max(1, streak) - 1) % cycle + 1;
    }

    public static DayOfWeek weekday(long day) {
        return LocalDate.ofEpochDay(day).getDayOfWeek();
    }

    /** Picks a fresh set of missions for {@code today}; {@code salt} lets debug commands re-roll. */
    public static DailyMissionData roll(ServerPlayer player, long today, long salt) {
        int cycleDay = cycleDay(player, today);
        RewardTable table = RewardTableManager.INSTANCE.forStreak(cycleDay);
        List<Identifier> forced = table == null ? List.of() : table.missions();
        return new DailyMissionData(today, pick(player.getUUID(), today ^ salt, MissionManager.INSTANCE.all(),
                ServerConfig.DAILY_MISSIONS_PER_DAY.get(), forced, cycleDay, weekday(today)));
    }

    public static List<DailyMissionData.Entry> pick(UUID player, long day, Map<Identifier, Mission> all, int count,
            List<Identifier> forced, int cycleDay, DayOfWeek weekday) {
        List<DailyMissionData.Entry> picked = new ArrayList<>();
        // Missions the reward table of this cycle day asks for always come first.
        for (Identifier id : forced) {
            if (all.containsKey(id) && picked.stream().noneMatch(e -> e.mission().equals(id))) {
                picked.add(new DailyMissionData.Entry(id, 0, false));
            }
        }
        List<Map.Entry<Identifier, Mission>> pool = new ArrayList<>(all.entrySet().stream()
                .filter(e -> e.getValue().availableOn(cycleDay, weekday) && !forced.contains(e.getKey())).toList());
        pool.sort(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)));
        RandomSource random = RandomSource.create(player.getMostSignificantBits() ^ player.getLeastSignificantBits() ^ (day * 0x9E3779B97F4A7C15L));
        while (picked.size() < count && !pool.isEmpty()) {
            int total = pool.stream().mapToInt(e -> e.getValue().weight()).sum();
            int roll = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                roll -= pool.get(i).getValue().weight();
                if (roll < 0) {
                    picked.add(new DailyMissionData.Entry(pool.remove(i).getKey(), 0, false));
                    break;
                }
            }
        }
        return List.copyOf(picked);
    }

    /** Adds progress to every open mission of this objective whose target matches. */
    public static void progress(ServerPlayer player, MissionObjective objective, Predicate<Mission> matches, long amount) {
        if (!enabled() || amount <= 0) {
            return;
        }
        DailyMissionData data = current(player, Instant.now());
        Map<Identifier, Mission> all = MissionManager.INSTANCE.all();
        for (int i = 0; i < data.missions().size(); i++) {
            DailyMissionData.Entry entry = data.missions().get(i);
            Mission mission = all.get(entry.mission());
            if (entry.completed() || mission == null || mission.objective() != objective || !matches.test(mission)) {
                continue;
            }
            data = addProgress(player, data, i, mission, amount);
        }
    }

    /** Debug: add progress to the mission at {@code index} of today's list. Returns false if there is none. */
    public static boolean progressAt(ServerPlayer player, int index, long amount) {
        DailyMissionData data = current(player, Instant.now());
        if (index < 0 || index >= data.missions().size()) {
            return false;
        }
        DailyMissionData.Entry entry = data.missions().get(index);
        Mission mission = MissionManager.INSTANCE.all().get(entry.mission());
        if (mission == null || entry.completed()) {
            return mission != null;
        }
        addProgress(player, data, index, mission, amount <= 0 ? mission.amount() : amount);
        return true;
    }

    private static DailyMissionData addProgress(ServerPlayer player, DailyMissionData data, int index, Mission mission, long amount) {
        DailyMissionData.Entry entry = data.missions().get(index);
        long progress = Math.min(mission.amount(), entry.progress() + amount);
        boolean done = progress >= mission.amount();
        data = data.with(index, new DailyMissionData.Entry(entry.mission(), progress, done));
        // Store before paying out, so a reward is never handed out twice.
        player.setData(DATA, data);
        if (done) {
            complete(player, mission);
        }
        return data;
    }

    /** For machines and other sources that only know the owner's UUID; does nothing if they're offline. */
    public static void progress(@Nullable MinecraftServer server, @Nullable UUID owner, MissionObjective objective, Predicate<Mission> matches, long amount) {
        if (server == null || owner == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            progress(player, objective, matches, amount);
        }
    }

    private static void complete(ServerPlayer player, Mission mission) {
        player.sendSystemMessage(Component.translatable("message.gsmod.mission.completed", mission.displayName()).withStyle(ChatFormatting.GREEN));
        Rewards.give(player, mission.rewards().stream().map(ItemStackTemplate::create).toList(), mission.experience());
    }

    private DailyMissions() {}
}
