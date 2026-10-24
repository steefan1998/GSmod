package nl.gsmod.machine;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import nl.gsmod.Config;
import nl.gsmod.daily.MissionObjective;

/**
 * A machine that consumes FE every tick while working on a recipe.
 */
public abstract class ProcessingBlockEntity<R> extends MachineBlockEntity {
    protected ProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineType machineType) {
        super(type, pos, state, machineType, Integer.MAX_VALUE, 0);
    }

    protected abstract @Nullable R findRecipe(ServerLevel level);

    protected abstract ItemStack resultOf(R recipe);

    protected abstract int processingTime(R recipe);

    /** Recipe-specified FE/tick, or -1 to use the machine's config default. */
    protected abstract int recipeEnergy(R recipe);

    protected abstract int defaultEnergyPerTick();

    /** Consume the inputs and insert the result. */
    protected abstract void craft(ServerLevel level, R recipe, ItemStack result);

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.setLimits(Config.MACHINE_MAX_INPUT.get(), 0);

        R recipe = findRecipe(level);
        ItemStack result = recipe == null ? ItemStack.EMPTY : resultOf(recipe);
        boolean working = false;

        if (recipe != null && canOutput(result)) {
            maxProgress = Math.max(1, (int) Math.round(processingTime(recipe) * Config.PROCESSING_TIME_MULTIPLIER.get() / speedFactor()));
            int base = recipeEnergy(recipe) >= 0 ? recipeEnergy(recipe) : defaultEnergyPerTick();
            int cost = (int) Math.ceil(base * energyFactor());
            if (energy.consume(cost)) {
                working = true;
                progress++;
                if (progress >= maxProgress) {
                    ItemStack before = items.get(machineType.outputSlot()).copy();
                    craft(level, recipe, result);
                    progress = 0;
                    ItemStack after = items.get(machineType.outputSlot());
                    int produced = after.getCount() - (ItemStack.isSameItemSameComponents(before, after) ? before.getCount() : 0);
                    creditOwner(MissionObjective.USE_MACHINE, m -> m.matchesMachine(machineType), 1);
                    creditOwner(MissionObjective.PRODUCE_ITEM, m -> m.matchesItem(after), produced);
                }
                setChanged();
            }
        } else if (progress != 0 || maxProgress != 0) {
            progress = 0;
            maxProgress = 0;
            setChanged();
        }

        setLit(level, pos, state, working);
    }
}
