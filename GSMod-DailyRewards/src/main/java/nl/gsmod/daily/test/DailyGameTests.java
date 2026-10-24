package nl.gsmod.daily.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.daily.DailyClock;
import nl.gsmod.daily.DailyConfig;
import nl.gsmod.daily.DailyMissionData;
import nl.gsmod.daily.DailyMissions;
import nl.gsmod.daily.DailyRewardData;
import nl.gsmod.daily.DailyRewards;
import nl.gsmod.daily.GSModDaily;
import nl.gsmod.daily.MissionManager;
import nl.gsmod.daily.MissionObjective;
import nl.gsmod.daily.RewardTableManager;
import nl.gsmod.daily.WorldJsonManager;

/** Run with {@code ./gradlew :GSMod-DailyRewards:runGameTestServer}, or {@code /test runall gsmod_daily} in a dev world. */
public final class DailyGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, GSModDaily.MODID);

    static {
        FUNCTIONS.register("claim_rules", () -> DailyGameTests::claimRules);
        FUNCTIONS.register("mission_progress", () -> DailyGameTests::missionProgress);
        FUNCTIONS.register("tiers_and_world_files", () -> DailyGameTests::tiersAndWorldFiles);
        FUNCTIONS.register("missions_per_day", () -> DailyGameTests::missionsPerDay);
        FUNCTIONS.register("time_zone_and_dst", () -> DailyGameTests::timeZoneAndDst);
        FUNCTIONS.register("week_bonus_and_comeback", () -> DailyGameTests::weekBonusAndComeback);
    }

    private static void timeZoneAndDst(GameTestHelper helper) {
        ZoneId amsterdam = ZoneId.of("Europe/Amsterdam");
        // Summer (CEST, UTC+2): the day flips at 22:00 UTC.
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-07-01T21:59:59Z"), amsterdam, 0), LocalDate.parse("2026-07-01").toEpochDay(), "summer before");
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-07-01T22:00:00Z"), amsterdam, 0), LocalDate.parse("2026-07-02").toEpochDay(), "summer after");
        // Winter (CET, UTC+1): the day flips at 23:00 UTC.
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-12-01T22:59:59Z"), amsterdam, 0), LocalDate.parse("2026-12-01").toEpochDay(), "winter before");
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-12-01T23:00:00Z"), amsterdam, 0), LocalDate.parse("2026-12-02").toEpochDay(), "winter after");
        // The day clocks go back (25 hours) and forward (23 hours).
        helper.assertValueEqual(DailyClock.secondsUntilReset(Instant.parse("2026-10-24T22:00:00Z"), amsterdam, 0), 25 * 3600L, "25-hour day");
        helper.assertValueEqual(DailyClock.secondsUntilReset(Instant.parse("2026-03-28T23:00:00Z"), amsterdam, 0), 23 * 3600L, "23-hour day");
        // Reset hour 6 in New York (EDT, UTC-4) is 10:00 UTC.
        ZoneId newYork = ZoneId.of("America/New_York");
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-07-01T09:59:00Z"), newYork, 6), LocalDate.parse("2026-06-30").toEpochDay(), "NY before 6:00");
        helper.assertValueEqual(DailyClock.day(Instant.parse("2026-07-01T10:00:00Z"), newYork, 6), LocalDate.parse("2026-07-01").toEpochDay(), "NY at 6:00");
        // UTC stays the default.
        helper.assertValueEqual(DailyConfig.zone().getId(), "UTC", "default zone");
        helper.succeed();
    }

    private static void weekBonusAndComeback(GameTestHelper helper) {
        helper.assertTrue(DailyRewards.isWeekBonus(7, 7) && DailyRewards.isWeekBonus(14, 7), "week bonus on day 7 and 14");
        helper.assertTrue(!DailyRewards.isWeekBonus(6, 7) && !DailyRewards.isWeekBonus(7, 0), "no week bonus on day 6 or when off");
        DailyRewardData lost = new DailyRewardData(100, 5, 5);
        helper.assertTrue(DailyRewards.isComeback(lost, lost.claimed(110, 0), 3), "lost 5-day streak gives comeback");
        helper.assertTrue(!DailyRewards.isComeback(new DailyRewardData(100, 2, 2), new DailyRewardData(110, 1, 3), 3), "2-day streak is too short");
        helper.assertTrue(!DailyRewards.isComeback(DailyRewardData.EMPTY, DailyRewardData.EMPTY.claimed(110, 0), 3), "first claim is no comeback");

        var week = RewardTableManager.INSTANCE.bonus(RewardTableManager.WEEK_BONUS);
        var comeback = RewardTableManager.INSTANCE.bonus(RewardTableManager.COMEBACK_BONUS);
        helper.assertTrue(week != null && comeback != null, "bonus tables missing");
        helper.assertTrue(week.random().stream().anyMatch(r -> r.item().item().getRegisteredName().equals("minecraft:enchanted_book")),
                "week bonus mending book not parsed");

        // Real claims through a player: day 7 in a row, then a lost streak.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        long today = DailyRewards.today(Instant.now());
        player.setData(DailyRewards.DATA, new DailyRewardData(today - 1, 6, 6));
        var seventh = DailyRewards.claim(player, Instant.now());
        helper.assertTrue(seventh.streak() == 7 && seventh.bonuses().size() == 1
                && seventh.bonuses().get(0).type().equals(RewardTableManager.WEEK_BONUS), "day 7 should give the week bonus: " + seventh);
        player.setData(DailyRewards.DATA, new DailyRewardData(today - 5, 4, 10));
        var back = DailyRewards.claim(player, Instant.now());
        helper.assertTrue(back.streak() == 1 && back.bonuses().size() == 1 && back.bonuses().get(0).type().equals(RewardTableManager.COMEBACK_BONUS)
                && back.bonuses().get(0).days() == 4, "lost streak should give the comeback reward: " + back);
        helper.succeed();
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(DailyGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(GSModDaily.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            Identifier id = entry.getId();
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 400, 0, true);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
    }

    private static void claimRules(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Instant day1 = Instant.parse("2026-10-24T10:00:00Z");
        Instant day1Late = Instant.parse("2026-10-24T23:59:00Z");
        Instant day2 = Instant.parse("2026-10-25T00:01:00Z");
        Instant day5 = Instant.parse("2026-10-28T12:00:00Z");

        var first = DailyRewards.claim(player, day1);
        helper.assertValueEqual(first.status(), DailyRewards.Status.CLAIMED, "first claim");
        helper.assertValueEqual(first.streak(), 1, "first streak");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) >= 4, "day 1 reward not given");

        helper.assertValueEqual(DailyRewards.claim(player, day1Late).status(), DailyRewards.Status.ALREADY_CLAIMED, "same-day claim");
        var second = DailyRewards.claim(player, day2);
        helper.assertValueEqual(second.streak(), 2, "streak after next day");
        // Clock moved back to day 1: must not pay out again.
        helper.assertValueEqual(DailyRewards.claim(player, day1).status(), DailyRewards.Status.ALREADY_CLAIMED, "clock went back");
        var afterGap = DailyRewards.claim(player, day5);
        helper.assertValueEqual(afterGap.streak(), 1, "streak after missing days");
        helper.assertValueEqual(DailyRewards.data(player).totalClaims(), 3, "total claims");
        helper.succeed();
    }

    private static void missionProgress(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier mission = GSModDaily.id("mine_iron_ore");
        helper.assertTrue(MissionManager.INSTANCE.all().containsKey(mission), "default missions not loaded");
        long today = DailyRewards.today(Instant.now());
        player.setData(DailyMissions.DATA, new DailyMissionData(today, List.of(new DailyMissionData.Entry(mission, 0, false))));

        var ironOre = Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
        DailyMissions.progress(player, MissionObjective.MINE_BLOCK, m -> m.matchesBlock(Blocks.STONE.defaultBlockState()), 1);
        helper.assertValueEqual(player.getData(DailyMissions.DATA).missions().get(0).progress(), 0L, "stone counted as iron ore");
        for (int i = 0; i < 40; i++) {
            DailyMissions.progress(player, MissionObjective.MINE_BLOCK, m -> m.matchesBlock(ironOre), 1);
        }
        var entry = player.getData(DailyMissions.DATA).missions().get(0);
        helper.assertTrue(entry.completed() && entry.progress() == 32, "mission not completed: " + entry);
        helper.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 8, "mission reward paid exactly once");

        // A fresh day re-rolls a full set of distinct missions.
        player.setData(DailyMissions.DATA, DailyMissionData.EMPTY);
        var rolled = DailyMissions.current(player, Instant.now()).missions();
        helper.assertValueEqual(rolled.size(), 3, "missions per day");
        helper.assertValueEqual(rolled.stream().map(DailyMissionData.Entry::mission).distinct().count(), 3L, "distinct missions");
        helper.succeed();
    }

    private static void tiersAndWorldFiles(GameTestHelper helper) {
        RewardTableManager tables = RewardTableManager.INSTANCE;
        helper.assertValueEqual(tables.cycleLength(), 10, "cycle length");
        helper.assertValueEqual(tables.forStreak(5).tier().orElse(""), "iron", "day 5 tier");
        helper.assertValueEqual(tables.forStreak(7).tier().orElse(""), "diamond", "day 7 tier");
        helper.assertValueEqual(tables.forStreak(10).tier().orElse(""), "netherite", "day 10 tier");
        helper.assertValueEqual(tables.forStreak(15).tier().orElse(""), "iron", "day 15 wraps to day 5");

        // Random counts stay within min/max, and two rolls never pick the same entry twice.
        RandomSource random = RandomSource.create(42);
        for (int i = 0; i < 200; i++) {
            List<ItemStack> day5 = tables.forStreak(5).roll(random);
            helper.assertTrue(day5.size() == 3, "day 5 should give 1 fixed + 2 random stacks, got " + day5);
            int iron = day5.stream().filter(s -> s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(iron >= 4 && iron <= 12, "day 5 iron out of range: " + iron);
            long netherite = tables.forStreak(10).roll(random).stream().filter(s -> s.is(Items.NETHERITE_INGOT)).count();
            helper.assertTrue(netherite <= 1, "too much netherite");
        }

        // Modded entries, GS Mod: Tech items included, are only in the pool while their mod is installed.
        boolean hasMekanism = tables.forStreak(5).random().stream()
                .anyMatch(r -> r.item().item().getRegisteredName().equals("mekanism:upgrade_speed"));
        helper.assertValueEqual(hasMekanism, ModList.get().isLoaded("mekanism"), "mekanism entry present");
        boolean hasTech = tables.forStreak(6).random().stream()
                .anyMatch(r -> r.item().item().getRegisteredName().startsWith("gsmod:"));
        helper.assertValueEqual(hasTech, ModList.get().isLoaded("gsmod"), "GS Mod: Tech entries present");

        // The running world got its own editable copies.
        Path folder = WorldJsonManager.worldFolder(RewardTableManager.FOLDER);
        helper.assertTrue(folder != null && Files.isRegularFile(folder.resolve("day_05.json")), "world reward files missing in " + folder);
        Path missions = WorldJsonManager.worldFolder(MissionManager.FOLDER);
        helper.assertTrue(missions != null && Files.isRegularFile(missions.resolve("mine_iron_ore.json")), "world mission files missing in " + missions);
        helper.succeed();
    }

    private static void missionsPerDay(GameTestHelper helper) {
        var all = MissionManager.INSTANCE.all();
        Identifier ironOre = GSModDaily.id("mine_iron_ore");
        Identifier coalOre = GSModDaily.id("mine_coal_ore");
        Identifier weekend = GSModDaily.id("weekend_play_60_minutes");
        List<Identifier> forcedDay5 = RewardTableManager.INSTANCE.forStreak(5).missions();
        helper.assertTrue(forcedDay5.contains(ironOre), "day 5 should force mine_iron_ore, has " + forcedDay5);

        for (int seed = 0; seed < 50; seed++) {
            var uuid = new UUID(seed, seed * 31L);
            // Monday, cycle day 5: forced iron mission first, no early-days or weekend missions.
            var monday = DailyMissions.pick(uuid, seed, all, 3, forcedDay5, 5, DayOfWeek.MONDAY)
                    .stream().map(DailyMissionData.Entry::mission).toList();
            helper.assertTrue(monday.size() == 3 && monday.get(0).equals(ironOre), "forced mission missing: " + monday);
            helper.assertTrue(!monday.contains(coalOre) && !monday.contains(weekend), "mission outside its days: " + monday);
            // Cycle day 2 on a Saturday: the early-days and the weekend mission are both allowed.
            var saturday = DailyMissions.pick(uuid, seed, all, 50, List.of(), 2, DayOfWeek.SATURDAY)
                    .stream().map(DailyMissionData.Entry::mission).toList();
            helper.assertTrue(saturday.contains(coalOre) && saturday.contains(weekend), "allowed mission missing: " + saturday);
        }
        helper.succeed();
    }

    private DailyGameTests() {}
}
