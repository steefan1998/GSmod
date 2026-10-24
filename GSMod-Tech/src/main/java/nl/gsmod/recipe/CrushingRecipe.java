package nl.gsmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import nl.gsmod.registry.ModRecipes;

/**
 * {@code gsmod:crushing}: one input item, one output stack.
 *
 * <pre>
 * {
 *   "type": "gsmod:crushing",
 *   "ingredient": "#c:ores/iron",
 *   "result": { "id": "...", "count": 2 },
 *   "bonus_chance": 0.1,        // optional, chance for one extra output
 *   "processing_time": 200,     // optional, ticks
 *   "energy_per_tick": -1       // optional, -1 = use the config default
 * }
 * </pre>
 */
public record CrushingRecipe(Ingredient ingredient, ItemStackTemplate result, float bonusChance, int processingTime, int energyPerTick)
        implements Recipe<SingleRecipeInput> {

    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.floatRange(0, 1).optionalFieldOf("bonus_chance", 0f).forGetter(CrushingRecipe::bonusChance),
            Codec.intRange(1, 72000).optionalFieldOf("processing_time", 200).forGetter(CrushingRecipe::processingTime),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("energy_per_tick", -1).forGetter(CrushingRecipe::energyPerTick)
    ).apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
            ByteBufCodecs.FLOAT, CrushingRecipe::bonusChance,
            ByteBufCodecs.VAR_INT, CrushingRecipe::processingTime,
            ByteBufCodecs.VAR_INT, CrushingRecipe::energyPerTick,
            CrushingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return ModRecipes.CRUSHING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.FURNACE_MISC;
    }
}
