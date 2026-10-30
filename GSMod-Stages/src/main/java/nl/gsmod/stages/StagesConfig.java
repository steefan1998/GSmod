package nl.gsmod.stages;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world config: {@code <world>/serverconfig/gsmod_stages-server.toml}. NeoForge sends it to every client
 * that joins, so JEI on the client hides exactly the items the server locks.
 */
public final class StagesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LOCKED_MODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LOCKED_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_ITEMS;
    public static final ModConfigSpec.BooleanValue REQUIRE_PLAYER_UNLOCK;
    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASS;
    public static final ModConfigSpec.BooleanValue ANNOUNCE;

    static {
        BUILDER.comment("End-game stage: everything listed here stays locked until the Ender Dragon has been defeated in this world.",
                "Locked items cannot be used, placed or crafted, and are hidden in JEI.").push("endgame");
        LOCKED_MODS = BUILDER.comment("Mod ids whose items and blocks are locked, e.g. \"projecte\" for EMC.",
                        "Mods that are not installed are simply ignored.")
                .defineListAllowEmpty("lockedMods", List.of("projecte", "projectexpansion", "stellaris", "draconicevolution", "avaritia", "extendedcrafting"),
                        () -> "", o -> o instanceof String s && !s.isBlank());
        LOCKED_ITEMS = BUILDER.comment("Extra locked items from other mods: an item id (\"minecraft:elytra\") or an item tag (\"#c:nether_stars\")")
                .defineListAllowEmpty("lockedItems", List.of(), () -> "", o -> o instanceof String s && !s.isBlank());
        ALLOWED_ITEMS = BUILDER.comment("Items that stay usable even though their mod is in lockedMods; item ids or \"#tags\"")
                .defineListAllowEmpty("allowedItems", List.of(), () -> "", o -> o instanceof String s && !s.isBlank());
        REQUIRE_PLAYER_UNLOCK = BUILDER.comment("After the dragon is defeated, every player still unlocks the stage once themselves",
                        "(button on the Daily screen, or /stages unlock). false = everyone gets it right away.")
                .define("requirePlayerUnlock", true);
        CREATIVE_BYPASS = BUILDER.comment("Players in creative or spectator mode are never locked")
                .define("creativeBypass", true);
        ANNOUNCE = BUILDER.comment("Tell everyone in chat when the dragon is defeated and the stage opens")
                .define("announce", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StagesConfig() {}
}
