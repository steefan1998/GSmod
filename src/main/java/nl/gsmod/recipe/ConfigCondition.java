package nl.gsmod.recipe;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;
import nl.gsmod.Config;

/**
 * {@code {"type": "gsmod:config", "option": "ore_doubling"}} — passes when that recipe toggle in
 * gsmod-common.toml is enabled. Lets players and pack makers switch recipe groups off without a datapack.
 */
public record ConfigCondition(String option) implements ICondition {
    private static final Map<String, Supplier<Boolean>> OPTIONS = Map.of(
            "ore_doubling", Config.ORE_DOUBLING,
            "steel_alloying", Config.STEEL_ALLOYING,
            "mekanism_recipes", Config.MEKANISM_RECIPES,
            "enderio_recipes", Config.ENDERIO_RECIPES);

    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.validate(s -> OPTIONS.containsKey(s)
                    ? DataResult.success(s)
                    : DataResult.error(() -> "Unknown gsmod config option: " + s))
                    .fieldOf("option").forGetter(ConfigCondition::option)
    ).apply(i, ConfigCondition::new));

    @Override
    public boolean test(IContext context) {
        // Defaults to enabled if the config somehow isn't loaded yet.
        return !Config.SPEC.isLoaded() || OPTIONS.get(option).get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
