package nl.gsmod.registry;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.gsmod.GSMod;
import nl.gsmod.recipe.AlloyingRecipe;
import nl.gsmod.recipe.ConfigCondition;
import nl.gsmod.recipe.CrushingRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, GSMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, GSMod.MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, GSMod.MODID);

    public static final Supplier<RecipeType<CrushingRecipe>> CRUSHING = TYPES.register("crushing", RecipeType::simple);
    public static final Supplier<RecipeType<AlloyingRecipe>> ALLOYING = TYPES.register("alloying", RecipeType::simple);

    public static final Supplier<RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER = SERIALIZERS.register("crushing",
            () -> new RecipeSerializer<>(CrushingRecipe.CODEC, CrushingRecipe.STREAM_CODEC));
    public static final Supplier<RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER = SERIALIZERS.register("alloying",
            () -> new RecipeSerializer<>(AlloyingRecipe.CODEC, AlloyingRecipe.STREAM_CODEC));

    public static final Supplier<MapCodec<ConfigCondition>> CONFIG_CONDITION = CONDITIONS.register("config", () -> ConfigCondition.CODEC);

    private ModRecipes() {}
}
