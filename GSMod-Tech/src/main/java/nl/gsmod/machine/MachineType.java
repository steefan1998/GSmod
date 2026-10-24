package nl.gsmod.machine;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Slot layout of each machine. Slot order in the container is: inputs, output (if any), upgrades.
 */
public enum MachineType implements StringRepresentable {
    GENERATOR("generator", 1, false),
    CRUSHER("crusher", 1, true),
    ALLOY_SMELTER("alloy_smelter", 2, true);

    public static final Codec<MachineType> CODEC = StringRepresentable.fromEnum(MachineType::values);
    public static final int UPGRADE_SLOTS = 3;
    /** progress, maxProgress, energy (lo/hi), capacity (lo/hi), burn, maxBurn */
    public static final int DATA_COUNT = 8;

    private final String name;
    private final int inputSlots;
    private final boolean hasOutput;

    MachineType(String name, int inputSlots, boolean hasOutput) {
        this.name = name;
        this.inputSlots = inputSlots;
        this.hasOutput = hasOutput;
    }

    public int inputSlots() {
        return inputSlots;
    }

    public boolean hasOutput() {
        return hasOutput;
    }

    public int outputSlot() {
        return hasOutput ? inputSlots : -1;
    }

    public int firstUpgradeSlot() {
        return inputSlots + (hasOutput ? 1 : 0);
    }

    public int slotCount() {
        return firstUpgradeSlot() + UPGRADE_SLOTS;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
