package nl.gsmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.gsmod.Config;
import nl.gsmod.energy.GSEnergyHandler;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.registry.ModBlockEntities;
import nl.gsmod.registry.ModDataComponents;

/**
 * Stores FE. Accepts energy on every side and pushes it out of its front face.
 * Keeps its charge when broken via the {@code gsmod:energy} item component.
 */
public class EnergyCellBlockEntity extends BlockEntity {
    private final GSEnergyHandler energy = new GSEnergyHandler(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, this::onEnergyChanged);
    private int lastComparator = -1;

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CELL.get(), pos, state);
    }

    public GSEnergyHandler getEnergy() {
        return energy;
    }

    private void onEnergyChanged() {
        setChanged();
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, EnergyCellBlockEntity cell) {
        int transfer = Config.ENERGY_CELL_TRANSFER.get();
        cell.energy.setCapacity(Config.ENERGY_CELL_CAPACITY.get());
        cell.energy.setLimits(transfer, transfer);

        Direction front = state.getValue(MachineBlock.FACING);
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(front), front.getOpposite());
        if (target != null && cell.energy.getEnergy() > 0) {
            try (Transaction transaction = Transaction.openRoot()) {
                EnergyHandlerUtil.move(cell.energy, target, transfer, transaction);
                transaction.commit();
            }
        }

        int comparator = EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(cell.energy);
        if (comparator != cell.lastComparator) {
            cell.lastComparator = comparator;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        energy.set(Math.max(0, components.getOrDefault(ModDataComponents.ENERGY.get(), 0)));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.ENERGY.get(), energy.getEnergy());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
    }
}
