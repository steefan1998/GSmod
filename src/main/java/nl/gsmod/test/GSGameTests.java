package nl.gsmod.test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.block.EnergyCellBlockEntity;
import nl.gsmod.daily.DailyMissionData;
import nl.gsmod.daily.DailyMissions;
import nl.gsmod.daily.DailyRewards;
import nl.gsmod.daily.MissionManager;
import nl.gsmod.daily.MissionObjective;
import nl.gsmod.machine.AlloySmelterBlockEntity;
import nl.gsmod.machine.CrusherBlockEntity;
import nl.gsmod.machine.GeneratorBlockEntity;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.registry.ModBlocks;
import nl.gsmod.registry.ModItems;

/** Run with {@code ./gradlew runGameTestServer}, or {@code /test runall gsmod} in a dev world. */
public final class GSGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, GSMod.MODID);
    private static final BlockPos A = new BlockPos(1, 1, 1);
    private static final BlockPos B = new BlockPos(2, 1, 1);

    static {
        FUNCTIONS.register("crusher_doubles_ore", () -> GSGameTests::crusherDoublesOre);
        FUNCTIONS.register("alloy_smelter_makes_steel", () -> GSGameTests::alloySmelterMakesSteel);
        FUNCTIONS.register("generator_charges_cell", () -> GSGameTests::generatorChargesCell);
        FUNCTIONS.register("cell_powers_crusher", () -> GSGameTests::cellPowersCrusher);
        FUNCTIONS.register("speed_upgrade_shortens_time", () -> GSGameTests::speedUpgradeShortensTime);
        FUNCTIONS.register("crusher_needs_energy", () -> GSGameTests::crusherNeedsEnergy);
        FUNCTIONS.register("daily_reward_claim_rules", () -> GSGameTests::dailyRewardClaimRules);
        FUNCTIONS.register("daily_mission_progress", () -> GSGameTests::dailyMissionProgress);
    }

    private static void dailyRewardClaimRules(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Instant day1 = Instant.parse("2026-10-24T10:00:00Z");
        Instant day1Late = Instant.parse("2026-10-24T23:59:00Z");
        Instant day2 = Instant.parse("2026-10-25T00:01:00Z");
        Instant day5 = Instant.parse("2026-10-28T12:00:00Z");

        var first = DailyRewards.claim(player, day1);
        helper.assertValueEqual(first.status(), DailyRewards.Status.CLAIMED, "first claim");
        helper.assertValueEqual(first.streak(), 1, "first streak");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) >= 8, "day 1 reward not given");

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

    private static void dailyMissionProgress(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier mission = GSMod.id("mine_iron_ore");
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

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(GSGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(GSMod.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            Identifier id = entry.getId();
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 400, 0, true);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
    }

    private static void crusherDoublesOre(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.getEnergy().generate(10_000);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get()));
        helper.succeedWhen(() -> {
            ItemStack out = crusher.getItem(1);
            helper.assertTrue(out.is(ModItems.GALVANITE_DUST.get()) && out.getCount() >= 2, "expected >= 2 galvanite dust, got " + out);
            helper.assertTrue(crusher.getItem(0).isEmpty(), "input not consumed");
        });
    }

    private static void alloySmelterMakesSteel(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(A, AlloySmelterBlockEntity.class);
        smelter.getEnergy().generate(15_000);
        // Reversed slot order on purpose: recipes match in either order.
        smelter.setItem(0, new ItemStack(Items.COAL, 2));
        smelter.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(smelter.getItem(2).is(ModItems.STEEL_INGOT.get()), "no steel produced, got " + smelter.getItem(2));
            helper.assertTrue(smelter.getItem(0).isEmpty() && smelter.getItem(1).isEmpty(), "inputs not consumed");
        });
    }

    private static void generatorChargesCell(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(B, ModBlocks.ENERGY_CELL.get());
        GeneratorBlockEntity generator = helper.getBlockEntity(A, GeneratorBlockEntity.class);
        generator.setItem(0, new ItemStack(Items.COAL));
        EnergyCellBlockEntity cell = helper.getBlockEntity(B, EnergyCellBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(cell.getEnergy().getEnergy() >= 2_000,
                "cell only has " + cell.getEnergy().getEnergy() + " FE"));
    }

    private static void cellPowersCrusher(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.ENERGY_CELL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(B, ModBlocks.CRUSHER.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(A, EnergyCellBlockEntity.class);
        cell.getEnergy().generate(100_000);
        CrusherBlockEntity crusher = helper.getBlockEntity(B, CrusherBlockEntity.class);
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> helper.assertTrue(crusher.getItem(1).is(Items.GRAVEL), "crusher produced " + crusher.getItem(1)));
    }

    private static void speedUpgradeShortensTime(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.getEnergy().generate(20_000);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get(), 4));
        crusher.setItem(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 4));
        // 200 ticks base / (1 + 4 * 0.5) = 67 ticks per ore.
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(crusher.getItem(1).getCount() >= 2, "upgraded crusher too slow: " + crusher.getItem(1));
            helper.succeed();
        });
    }

    private static void crusherNeedsEnergy(GameTestHelper helper) {
        helper.setBlock(A, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(A, CrusherBlockEntity.class);
        crusher.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get()));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(crusher.getItem(1).isEmpty(), "crusher worked without power");
            helper.succeed();
        });
    }

    private GSGameTests() {}
}
