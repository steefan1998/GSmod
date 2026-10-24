package nl.gsmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config (config/gsmod-common.toml). Loaded before datapacks, so recipe conditions
 * ({@link nl.gsmod.recipe.ConfigCondition}) can read it.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Combustion generator ---
    public static final ModConfigSpec.IntValue GENERATOR_FE_PER_TICK;
    public static final ModConfigSpec.IntValue GENERATOR_CAPACITY;
    public static final ModConfigSpec.IntValue GENERATOR_MAX_OUTPUT;
    public static final ModConfigSpec.DoubleValue GENERATOR_BURN_TIME_MULTIPLIER;

    // --- Crusher ---
    public static final ModConfigSpec.IntValue CRUSHER_FE_PER_TICK;
    public static final ModConfigSpec.IntValue CRUSHER_CAPACITY;

    // --- Alloy smelter ---
    public static final ModConfigSpec.IntValue ALLOY_SMELTER_FE_PER_TICK;
    public static final ModConfigSpec.IntValue ALLOY_SMELTER_CAPACITY;

    // --- Machines in general ---
    public static final ModConfigSpec.IntValue MACHINE_MAX_INPUT;
    public static final ModConfigSpec.DoubleValue PROCESSING_TIME_MULTIPLIER;

    // --- Energy cell ---
    public static final ModConfigSpec.IntValue ENERGY_CELL_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_CELL_TRANSFER;

    // --- Upgrades ---
    public static final ModConfigSpec.IntValue MAX_UPGRADES_PER_SLOT;
    public static final ModConfigSpec.DoubleValue SPEED_UPGRADE_BONUS;
    public static final ModConfigSpec.DoubleValue SPEED_UPGRADE_ENERGY_PENALTY;
    public static final ModConfigSpec.DoubleValue EFFICIENCY_UPGRADE_REDUCTION;
    public static final ModConfigSpec.DoubleValue CAPACITY_UPGRADE_BONUS;
    public static final ModConfigSpec.DoubleValue YIELD_UPGRADE_CHANCE;

    // --- Recipes ---
    public static final ModConfigSpec.BooleanValue ORE_DOUBLING;
    public static final ModConfigSpec.BooleanValue STEEL_ALLOYING;
    public static final ModConfigSpec.BooleanValue MEKANISM_RECIPES;
    public static final ModConfigSpec.BooleanValue ENDERIO_RECIPES;

    static {
        BUILDER.comment("Combustion Generator").push("generator");
        GENERATOR_FE_PER_TICK = BUILDER.comment("FE produced per tick while burning")
                .defineInRange("fePerTick", 40, 1, 1_000_000);
        GENERATOR_CAPACITY = BUILDER.comment("Base energy buffer (FE)")
                .defineInRange("capacity", 50_000, 1, Integer.MAX_VALUE / 8);
        GENERATOR_MAX_OUTPUT = BUILDER.comment("Maximum FE pushed to neighbours per tick")
                .defineInRange("maxOutput", 200, 1, Integer.MAX_VALUE);
        GENERATOR_BURN_TIME_MULTIPLIER = BUILDER.comment("Multiplier on vanilla furnace burn time of a fuel")
                .defineInRange("burnTimeMultiplier", 1.0, 0.01, 100.0);
        BUILDER.pop();

        BUILDER.comment("Crusher").push("crusher");
        CRUSHER_FE_PER_TICK = BUILDER.comment("Default FE used per tick (recipes may override with \"energy_per_tick\")")
                .defineInRange("fePerTick", 20, 0, 1_000_000);
        CRUSHER_CAPACITY = BUILDER.comment("Base energy buffer (FE)")
                .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE / 8);
        BUILDER.pop();

        BUILDER.comment("Alloy Smelter").push("alloy_smelter");
        ALLOY_SMELTER_FE_PER_TICK = BUILDER.comment("Default FE used per tick (recipes may override with \"energy_per_tick\")")
                .defineInRange("fePerTick", 30, 0, 1_000_000);
        ALLOY_SMELTER_CAPACITY = BUILDER.comment("Base energy buffer (FE)")
                .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE / 8);
        BUILDER.pop();

        BUILDER.comment("Settings shared by all processing machines").push("machines");
        MACHINE_MAX_INPUT = BUILDER.comment("Maximum FE a machine accepts per tick")
                .defineInRange("maxInput", 1_000, 1, Integer.MAX_VALUE);
        PROCESSING_TIME_MULTIPLIER = BUILDER.comment("Multiplier on every recipe's processing_time")
                .defineInRange("processingTimeMultiplier", 1.0, 0.01, 100.0);
        BUILDER.pop();

        BUILDER.comment("Energy Cell").push("energy_cell");
        ENERGY_CELL_CAPACITY = BUILDER.comment("Energy cell capacity (FE)")
                .defineInRange("capacity", 1_000_000, 1, Integer.MAX_VALUE);
        ENERGY_CELL_TRANSFER = BUILDER.comment("Maximum FE in/out per tick")
                .defineInRange("transfer", 1_000, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Machine upgrades").push("upgrades");
        MAX_UPGRADES_PER_SLOT = BUILDER.comment("How many upgrades stack in a single upgrade slot")
                .defineInRange("maxPerSlot", 4, 1, 64);
        SPEED_UPGRADE_BONUS = BUILDER.comment("Extra speed per speed upgrade (0.5 = +50%)")
                .defineInRange("speedBonus", 0.5, 0.0, 10.0);
        SPEED_UPGRADE_ENERGY_PENALTY = BUILDER.comment("Extra FE/tick per speed upgrade (0.6 = +60%)")
                .defineInRange("speedEnergyPenalty", 0.6, 0.0, 10.0);
        EFFICIENCY_UPGRADE_REDUCTION = BUILDER.comment("Energy reduction per efficiency upgrade for consumers, extra FE per fuel for generators (0.15 = 15%)")
                .defineInRange("efficiencyReduction", 0.15, 0.0, 1.0);
        CAPACITY_UPGRADE_BONUS = BUILDER.comment("Extra energy buffer per capacity upgrade (1.0 = +100%)")
                .defineInRange("capacityBonus", 1.0, 0.0, 100.0);
        YIELD_UPGRADE_CHANCE = BUILDER.comment("Chance per yield upgrade for one extra output item")
                .defineInRange("yieldChance", 0.1, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Toggle groups of built-in recipes. Changes apply on /reload or world load.").push("recipes");
        ORE_DOUBLING = BUILDER.comment("Crusher doubles ores (one ore -> two dusts or raw items)")
                .define("oreDoubling", true);
        STEEL_ALLOYING = BUILDER.comment("Alloy smelter can make steel from iron and coal")
                .define("steelAlloying", true);
        MEKANISM_RECIPES = BUILDER.comment("Add compatibility recipes when Mekanism is installed")
                .define("mekanismRecipes", true);
        ENDERIO_RECIPES = BUILDER.comment("Add compatibility recipes when Ender IO is installed")
                .define("enderioRecipes", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
