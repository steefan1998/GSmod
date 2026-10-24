package nl.gsmod.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.gsmod.Config;
import nl.gsmod.item.UpgradeType;
import nl.gsmod.registry.ModBlockEntities;

/**
 * Burns any furnace fuel into FE and pushes it to all adjacent energy handlers.
 * Speed upgrades burn faster (more FE/tick, same FE per item); efficiency upgrades give more FE per item.
 */
public class GeneratorBlockEntity extends MachineBlockEntity {
    private static final int FUEL_SLOT = 0;

    /** Remaining burn time in vanilla fuel ticks. */
    private float burnRemaining;
    private int burnTotal;
    private long pendingMissionEnergy;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state, MachineType.GENERATOR, 0, Integer.MAX_VALUE);
    }

    @Override
    protected int baseCapacity() {
        return Config.GENERATOR_CAPACITY.get();
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.setLimits(0, Config.GENERATOR_MAX_OUTPUT.get());
        boolean hasRoom = energy.getEnergy() < energy.getCapacity();

        if (burnRemaining <= 0 && hasRoom) {
            ItemStack fuel = items.get(FUEL_SLOT);
            int burnTime = getBurnTime(level, fuel);
            if (burnTime > 0) {
                burnTotal = (int) Math.max(1, burnTime * Config.GENERATOR_BURN_TIME_MULTIPLIER.get());
                burnRemaining = burnTotal;
                ItemStackTemplate remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    items.set(FUEL_SLOT, remainder != null ? remainder.create() : ItemStack.EMPTY);
                }
                setChanged();
                reportOperation();
            }
        }

        boolean burning = burnRemaining > 0;
        if (burning && hasRoom) {
            double speed = speedFactor();
            double efficiency = 1.0 + upgrades(UpgradeType.EFFICIENCY) * Config.EFFICIENCY_UPGRADE_REDUCTION.get();
            pendingMissionEnergy += energy.generate((int) Math.round(Config.GENERATOR_FE_PER_TICK.get() * speed * efficiency));
            burnRemaining = (float) Math.max(0, burnRemaining - speed);
            setChanged();
        }
        // Report generated FE to missions once a second instead of every tick.
        if (pendingMissionEnergy > 0 && level.getGameTime() % 20 == 0) {
            reportEnergy(pendingMissionEnergy);
            pendingMissionEnergy = 0;
        }

        pushEnergy(level, pos);
        setLit(level, pos, state, burning);
    }

    private void pushEnergy(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (energy.getEnergy() <= 0) {
                return;
            }
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                try (Transaction transaction = Transaction.openRoot()) {
                    EnergyHandlerUtil.move(energy, target, Config.GENERATOR_MAX_OUTPUT.get(), transaction);
                    transaction.commit();
                }
            }
        }
    }

    private static int getBurnTime(ServerLevel level, ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    @Override
    protected boolean isValidInput(int slot, ItemStack stack) {
        return level != null && stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    @Override
    protected int getBurnDisplay() {
        return (int) Math.ceil(burnRemaining);
    }

    @Override
    protected int getMaxBurnDisplay() {
        return burnTotal;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnRemaining = input.getFloatOr("burn_remaining", 0);
        burnTotal = input.getIntOr("burn_total", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("burn_remaining", burnRemaining);
        output.putInt("burn_total", burnTotal);
    }
}
