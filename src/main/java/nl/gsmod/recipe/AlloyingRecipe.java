package nl.gsmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import nl.gsmod.registry.ModRecipes;

/**
 * {@code gsmod:alloying}: two sized inputs (in either slot order) into one output stack.
 *
 * <pre>
 * {
 *   "type": "gsmod:alloying",
 *   "first":  { "ingredient": "#c:ingots/iron", "count": 1 },
 *   "second": { "ingredient": "minecraft:coal", "count": 2 },
 *   "result": { "id": "gsmod:steel_ingot" },
 *   "processing_time": 300,     // optional, ticks
 *   "energy_per_tick": -1       // optional, -1 = use the config default
 * }
 * </pre>
 */
public record AlloyingRecipe(SizedIngredient first, SizedIngredient second, ItemStackTemplate result, int processingTime, int energyPerTick)
        implements Recipe<AlloyInput> {

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            SizedIngredient.NESTED_CODEC.fieldOf("first").forGetter(AlloyingRecipe::first),
            SizedIngredient.NESTED_CODEC.fieldOf("second").forGetter(AlloyingRecipe::second),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            Codec.intRange(1, 72000).optionalFieldOf("processing_time", 200).forGetter(AlloyingRecipe::processingTime),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("energy_per_tick", -1).forGetter(AlloyingRecipe::energyPerTick)
    ).apply(i, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, AlloyingRecipe::first,
            SizedIngredient.STREAM_CODEC, AlloyingRecipe::second,
            ItemStackTemplate.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::processingTime,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::energyPerTick,
            AlloyingRecipe::new);

    @Override
    public boolean matches(AlloyInput input, Level level) {
        return consumption(input) != null;
    }

    /**
     * How many items to take from {@code [slot 0, slot 1]}, or {@code null} if the input doesn't match.
     */
    public int[] consumption(AlloyInput input) {
        if (first.test(input.first()) && second.test(input.second())) {
            return new int[] { first.count(), second.count() };
        }
        if (second.test(input.first()) && first.test(input.second())) {
            return new int[] { second.count(), first.count() };
        }
        return null;
    }

    @Override
    public ItemStack assemble(AlloyInput input) {
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
    public RecipeSerializer<AlloyingRecipe> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AlloyingRecipe> getType() {
        return ModRecipes.ALLOYING.get();
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
