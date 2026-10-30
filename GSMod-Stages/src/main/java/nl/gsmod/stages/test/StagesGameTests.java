package nl.gsmod.stages.test;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.stages.GSModStages;
import nl.gsmod.stages.Stages;

/** Run with {@code ./gradlew :GSMod-Stages:runGameTestServer}, or {@code /test runall gsmod_stages} in a dev world. */
public final class StagesGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, GSModStages.MODID);

    static {
        FUNCTIONS.register("locked_items", () -> StagesGameTests::lockedItems);
        FUNCTIONS.register("world_and_player_unlock", () -> StagesGameTests::worldAndPlayerUnlock);
    }

    private static void lockedItems(GameTestHelper helper) {
        Holder<Item> diamond = Items.DIAMOND.builtInRegistryHolder();
        Holder<Item> log = Items.OAK_LOG.builtInRegistryHolder();
        helper.assertTrue(Stages.matches(diamond, List.of("minecraft"), List.of(), List.of()), "locked by mod id");
        helper.assertTrue(!Stages.matches(diamond, List.of("projecte"), List.of(), List.of()), "other mod is not locked");
        helper.assertTrue(Stages.matches(diamond, List.of(), List.of("minecraft:diamond"), List.of()), "locked by item id");
        helper.assertTrue(Stages.matches(log, List.of(), List.of("#minecraft:logs"), List.of()), "locked by tag");
        helper.assertTrue(!Stages.matches(diamond, List.of("minecraft"), List.of(), List.of("minecraft:diamond")), "allowed item stays usable");
        helper.assertTrue(!Stages.matches(log, List.of("minecraft"), List.of(), List.of("#minecraft:logs")), "allowed tag stays usable");
        // Default config only locks mods that are not installed here.
        helper.assertTrue(!Stages.isLocked(Items.DIAMOND.getDefaultInstance()), "vanilla items are not locked by default");
        helper.succeed();
    }

    private static void worldAndPlayerUnlock(GameTestHelper helper) {
        helper.assertTrue(!Stages.access(false, true), "no access before the dragon, even after unlocking");
        helper.assertTrue(!Stages.access(true, false), "dragon alone is not enough, the player unlocks once");
        helper.assertTrue(Stages.access(true, true), "access after dragon and unlock");

        // The flags are stored on the overworld and the player.
        MinecraftServer server = helper.getLevel().getServer();
        boolean wasOpen = Stages.worldOpen(server);
        try {
            Stages.setWorldOpen(server, true);
            helper.assertTrue(Stages.worldOpen(server), "world flag stored");
        } finally {
            Stages.setWorldOpen(server, wasOpen);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!Stages.playerUnlocked(player), "players start locked");
        Stages.setPlayerUnlocked(player, true);
        helper.assertTrue(Stages.playerUnlocked(player), "player flag stored");
        // The mock player is in creative mode, which bypasses the lock.
        helper.assertTrue(player.isCreative() && Stages.bypass(player) && Stages.hasAccess(player), "creative bypasses the lock");
        helper.succeed();
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(StagesGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(GSModStages.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            Identifier id = entry.getId();
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 400, 0, true);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
    }

    private StagesGameTests() {}
}
