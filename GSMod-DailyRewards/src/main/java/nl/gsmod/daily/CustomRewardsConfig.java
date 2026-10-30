package nl.gsmod.daily;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Own reward tables for every world: {@code config/gsmod_daily/custom_rewards/*.json}, same format as
 * {@code <world>/gsmod_daily/daily_rewards/}. Settings are in {@code config/gsmod_daily/custom_rewards.toml},
 * and {@code config/gsmod_daily/uitleg/} explains the format with example files.
 */
public final class CustomRewardsConfig {
    public static final String FILE_NAME = GSModDaily.MODID + "/custom_rewards.toml";

    public enum Mode {
        /** A custom day or bonus replaces the world's table for that day or bonus; the other days stay. */
        MERGE,
        /** Only the custom tables are used (if there are any). */
        REPLACE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.EnumValue<Mode> MODE;

    static {
        BUILDER.comment("Your own daily rewards, used in every world.",
                "Put reward JSON files in config/gsmod_daily/custom_rewards/; see config/gsmod_daily/uitleg/ for how they work.",
                "Changes to the JSON files apply with /reload or /daily debug reload.").push("customRewards");
        ENABLED = BUILDER.comment("Use the files in config/gsmod_daily/custom_rewards/")
                .define("enabled", true);
        MODE = BUILDER.comment("MERGE: a custom day (or week/comeback bonus) replaces the same day of the world, all other days stay.",
                        "       A custom day higher than the last world day makes the cycle longer.",
                        "REPLACE: only the custom files are used; the world's own reward files are ignored.",
                        "         Falls back to the world's files when the custom folder has no valid reward days.")
                .defineEnum("mode", Mode.MERGE);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Copied from the jar to config/gsmod_daily/ when missing; "/gsmod_daily-config/" is not a package, so it is never encapsulated.
    private static final String RESOURCE_ROOT = "/gsmod_daily-config/";
    private static final List<String> FILES = List.of(
            "custom_rewards/LEES_EERST.txt",
            "uitleg/LEESMIJ.txt",
            "uitleg/README_EN.txt",
            "uitleg/voorbeelden/day_01_simpel.json",
            "uitleg/voorbeelden/day_05_willekeurig.json",
            "uitleg/voorbeelden/day_07_speciale_items.json",
            "uitleg/voorbeelden/day_10_met_mods.json",
            "uitleg/voorbeelden/bonus_week.json",
            "uitleg/voorbeelden/bonus_comeback.json");

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(GSModDaily.MODID);
    }

    public static Path rewardsFolder() {
        return folder().resolve("custom_rewards");
    }

    public static boolean enabled() {
        return !SPEC.isLoaded() || ENABLED.get();
    }

    public static Mode mode() {
        return SPEC.isLoaded() ? MODE.get() : Mode.MERGE;
    }

    /** Creates {@code config/gsmod_daily/} with the explanation and examples; files that already exist are left alone. */
    public static void writeDefaults() {
        Path root = folder();
        for (String name : FILES) {
            Path file = root.resolve(name);
            if (Files.exists(file)) {
                continue;
            }
            try (InputStream in = CustomRewardsConfig.class.getResourceAsStream(RESOURCE_ROOT + name)) {
                if (in == null) {
                    GSModDaily.LOGGER.warn("Missing built-in file {}", name);
                    continue;
                }
                Files.createDirectories(file.getParent());
                Files.copy(in, file);
            } catch (IOException e) {
                GSModDaily.LOGGER.warn("Could not write {}", file, e);
            }
        }
    }

    private CustomRewardsConfig() {}
}
