package nl.gsmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import nl.gsmod.Config;
import nl.gsmod.item.UpgradeItem;
import nl.gsmod.machine.MachineType;
import nl.gsmod.registry.ModMenus;

public class MachineMenu extends AbstractContainerMenu {
    public static final int UPGRADE_X = 152;

    private final MachineType machineType;
    private final Container container;
    private final ContainerData data;
    private final int machineSlots;

    /** Client-side constructor. */
    public MachineMenu(MachineType machineType, int containerId, Inventory inventory) {
        this(machineType, containerId, inventory, new SimpleContainer(machineType.slotCount()), new SimpleContainerData(MachineType.DATA_COUNT));
    }

    public MachineMenu(MachineType machineType, int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.forType(machineType), containerId);
        checkContainerSize(container, machineType.slotCount());
        checkContainerDataCount(data, MachineType.DATA_COUNT);
        this.machineType = machineType;
        this.container = container;
        this.data = data;
        this.machineSlots = machineType.slotCount();

        switch (machineType) {
            case GENERATOR -> addSlot(new Slot(container, 0, 80, 45));
            case CRUSHER -> {
                addSlot(new Slot(container, 0, 56, 35));
                addSlot(new OutputSlot(container, 1, 116, 35));
            }
            case ALLOY_SMELTER -> {
                addSlot(new Slot(container, 0, 38, 35));
                addSlot(new Slot(container, 1, 56, 35));
                addSlot(new OutputSlot(container, 2, 116, 35));
            }
        }
        for (int i = 0; i < MachineType.UPGRADE_SLOTS; i++) {
            addSlot(new UpgradeSlot(container, machineType.firstUpgradeSlot() + i, UPGRADE_X, 17 + i * 18));
        }

        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getEnergy() {
        return (data.get(2) & 0xFFFF) | (data.get(3) << 16);
    }

    public int getCapacity() {
        return (data.get(4) & 0xFFFF) | (data.get(5) << 16);
    }

    public int getBurn() {
        return data.get(6);
    }

    public int getMaxBurn() {
        return data.get(7);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots;
        int hotbarStart = playerStart + 27;
        int playerEnd = playerStart + 36;

        if (slotIndex < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (stack.getItem() instanceof UpgradeItem) {
            if (!moveItemStackTo(stack, machineType.firstUpgradeSlot(), machineSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineType.inputSlots(), false)) {
            boolean moved = slotIndex < hotbarStart
                    ? moveItemStackTo(stack, hotbarStart, playerEnd, false)
                    : moveItemStackTo(stack, playerStart, hotbarStart, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }

        @Override
        public int getMaxStackSize() {
            return Config.SPEC.isLoaded() ? Config.MAX_UPGRADES_PER_SLOT.get() : 4;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
    }
}
