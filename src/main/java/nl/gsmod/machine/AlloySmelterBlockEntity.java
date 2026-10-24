package nl.gsmod.machine;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import nl.gsmod.Config;
import nl.gsmod.recipe.AlloyInput;
import nl.gsmod.recipe.AlloyingRecipe;
import nl.gsmod.registry.ModBlockEntities;
import nl.gsmod.registry.ModRecipes;

public class AlloySmelterBlockEntity extends ProcessingBlockEntity<AlloyingRecipe> {
    private final RecipeManager.CachedCheck<AlloyInput, AlloyingRecipe> quickCheck = RecipeManager.createCheck(ModRecipes.ALLOYING.get());

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state, MachineType.ALLOY_SMELTER);
    }

    private AlloyInput input() {
        return new AlloyInput(items.get(0), items.get(1));
    }

    @Override
    protected int baseCapacity() {
        return Config.ALLOY_SMELTER_CAPACITY.get();
    }

    @Override
    protected int defaultEnergyPerTick() {
        return Config.ALLOY_SMELTER_FE_PER_TICK.get();
    }

    @Override
    protected @Nullable AlloyingRecipe findRecipe(ServerLevel level) {
        AlloyInput input = input();
        if (input.first().isEmpty() || input.second().isEmpty()) {
            return null;
        }
        return quickCheck.getRecipeFor(input, level).map(RecipeHolder::value).orElse(null);
    }

    @Override
    protected ItemStack resultOf(AlloyingRecipe recipe) {
        return recipe.assemble(input());
    }

    @Override
    protected int processingTime(AlloyingRecipe recipe) {
        return recipe.processingTime();
    }

    @Override
    protected int recipeEnergy(AlloyingRecipe recipe) {
        return recipe.energyPerTick();
    }

    @Override
    protected void craft(ServerLevel level, AlloyingRecipe recipe, ItemStack result) {
        int[] consumed = recipe.consumption(input());
        if (consumed == null) {
            return;
        }
        items.get(0).shrink(consumed[0]);
        items.get(1).shrink(consumed[1]);
        addOutput(result);
        rollBonus(level, result, 0f);
    }
}
