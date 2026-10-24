package nl.gsmod.machine;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.gsmod.Config;
import nl.gsmod.daily.DailyMissions;
import nl.gsmod.daily.Mission;
import nl.gsmod.daily.MissionObjective;
import nl.gsmod.energy.GSEnergyHandler;
import nl.gsmod.item.UpgradeItem;
import nl.gsmod.item.UpgradeType;
import nl.gsmod.menu.MachineMenu;

/**
 * Shared logic for the three machines: inventory (inputs, optional output, three upgrade slots),
 * an FE buffer, progress tracking and the data synced to the open menu.
 */
public abstract class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    protected final MachineType machineType;
    protected NonNullList<ItemStack> items;
    protected final GSEnergyHandler energy;
    protected int progress;
    protected int maxProgress;
    /** Player who placed the machine; credited for daily missions. */
    protected @Nullable UUID owner;

    private final int[] automationSlots;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergy() & 0xFFFF;
                case 3 -> energy.getEnergy() >>> 16;
                case 4 -> energy.getCapacity() & 0xFFFF;
                case 5 -> energy.getCapacity() >>> 16;
                case 6 -> getBurnDisplay();
                case 7 -> getMaxBurnDisplay();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Only read on the client through SimpleContainerData.
        }

        @Override
        public int getCount() {
            return MachineType.DATA_COUNT;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineType machineType, int maxInsert, int maxExtract) {
        super(type, pos, state);
        this.machineType = machineType;
        this.items = NonNullList.withSize(machineType.slotCount(), ItemStack.EMPTY);
        // The real capacity is applied on the first tick, when the config is guaranteed to be loaded.
        this.energy = new GSEnergyHandler(Integer.MAX_VALUE / 8, maxInsert, maxExtract, this::setChanged);
        int last = machineType.hasOutput() ? machineType.outputSlot() : machineType.inputSlots() - 1;
        this.automationSlots = IntStream.rangeClosed(0, last).toArray();
    }

    public GSEnergyHandler getEnergy() {
        return energy;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    protected void creditOwner(MissionObjective objective, Predicate<Mission> matches, long amount) {
        if (level != null) {
            DailyMissions.progress(level.getServer(), owner, objective, matches, amount);
        }
    }

    /** Base buffer size from the config, before capacity upgrades. */
    protected abstract int baseCapacity();

    protected abstract void serverTick(ServerLevel level, BlockPos pos, BlockState state);

    protected int getBurnDisplay() {
        return 0;
    }

    protected int getMaxBurnDisplay() {
        return 0;
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.energy.setCapacity((int) Math.min(Integer.MAX_VALUE,
                machine.baseCapacity() * (1.0 + machine.upgrades(UpgradeType.CAPACITY) * Config.CAPACITY_UPGRADE_BONUS.get())));
        machine.serverTick(level, pos, state);
    }

    protected void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(MachineBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), 3);
        }
    }

    // --- Upgrades ---

    public int upgrades(UpgradeType type) {
        int count = 0;
        for (int i = machineType.firstUpgradeSlot(); i < machineType.slotCount(); i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    protected double speedFactor() {
        return 1.0 + upgrades(UpgradeType.SPEED) * Config.SPEED_UPGRADE_BONUS.get();
    }

    /** FE/tick multiplier for consumers: speed costs more, efficiency costs less (never below 10%). */
    protected double energyFactor() {
        double speed = 1.0 + upgrades(UpgradeType.SPEED) * Config.SPEED_UPGRADE_ENERGY_PENALTY.get();
        double efficiency = Math.max(0.1, 1.0 - upgrades(UpgradeType.EFFICIENCY) * Config.EFFICIENCY_UPGRADE_REDUCTION.get());
        return speed * efficiency;
    }

    // --- Output helpers ---

    protected boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack current = items.get(machineType.outputSlot());
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= Math.min(current.getMaxStackSize(), getMaxStackSize());
    }

    protected void addOutput(ItemStack result) {
        int slot = machineType.outputSlot();
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            items.set(slot, result.copy());
        } else {
            current.grow(result.getCount());
        }
    }

    /** Adds the yield-upgrade / recipe bonus item if there is room. */
    protected void rollBonus(ServerLevel level, ItemStack result, float recipeBonusChance) {
        double chance = recipeBonusChance + upgrades(UpgradeType.YIELD) * Config.YIELD_UPGRADE_CHANCE.get();
        while (chance > 0) {
            if (level.getRandom().nextDouble() < chance) {
                ItemStack bonus = result.copyWithCount(1);
                if (canOutput(bonus)) {
                    addOutput(bonus);
                }
            }
            chance -= 1.0;
        }
    }

    // --- Save / load ---

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.deserialize(input);
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("max_progress", 0);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        energy.serialize(output);
        output.putInt("progress", progress);
        output.putInt("max_progress", maxProgress);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
    }

    // --- Container ---

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return machineType.slotCount();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MachineMenu(machineType, containerId, inventory, this, data);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= machineType.firstUpgradeSlot()) {
            return stack.getItem() instanceof UpgradeItem;
        }
        if (slot == machineType.outputSlot()) {
            return false;
        }
        return isValidInput(slot, stack);
    }

    protected boolean isValidInput(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return automationSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < machineType.inputSlots() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == machineType.outputSlot()) {
            return true;
        }
        // Lets hoppers pull out leftovers such as empty buckets from the generator.
        return slot < machineType.inputSlots() && !isValidInput(slot, stack);
    }
}
