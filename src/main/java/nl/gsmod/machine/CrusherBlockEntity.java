package nl.gsmod.machine;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import nl.gsmod.Config;
import nl.gsmod.recipe.CrushingRecipe;
import nl.gsmod.registry.ModBlockEntities;
import nl.gsmod.registry.ModRecipes;

public class CrusherBlockEntity extends ProcessingBlockEntity<CrushingRecipe> {
    private static final int INPUT_SLOT = 0;

    private final RecipeManager.CachedCheck<SingleRecipeInput, CrushingRecipe> quickCheck = RecipeManager.createCheck(ModRecipes.CRUSHING.get());

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state, MachineType.CRUSHER);
    }

    @Override
    protected int baseCapacity() {
        return Config.CRUSHER_CAPACITY.get();
    }

    @Override
    protected int defaultEnergyPerTick() {
        return Config.CRUSHER_FE_PER_TICK.get();
    }

    @Override
    protected @Nullable CrushingRecipe findRecipe(ServerLevel level) {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return null;
        }
        return quickCheck.getRecipeFor(new SingleRecipeInput(input), level).map(RecipeHolder::value).orElse(null);
    }

    @Override
    protected ItemStack resultOf(CrushingRecipe recipe) {
        return recipe.assemble(new SingleRecipeInput(items.get(INPUT_SLOT)));
    }

    @Override
    protected int processingTime(CrushingRecipe recipe) {
        return recipe.processingTime();
    }

    @Override
    protected int recipeEnergy(CrushingRecipe recipe) {
        return recipe.energyPerTick();
    }

    @Override
    protected void craft(ServerLevel level, CrushingRecipe recipe, ItemStack result) {
        items.get(INPUT_SLOT).shrink(1);
        addOutput(result);
        rollBonus(level, result, recipe.bonusChance());
    }
}
