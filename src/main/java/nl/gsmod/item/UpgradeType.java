package nl.gsmod.item;

import net.minecraft.util.StringRepresentable;

public enum UpgradeType implements StringRepresentable {
    /** Faster processing / burning, at a higher FE cost per tick. */
    SPEED("speed"),
    /** Less FE per operation for consumers; more FE per fuel item for generators. */
    EFFICIENCY("efficiency"),
    /** Larger internal energy buffer. */
    CAPACITY("capacity"),
    /** Chance for an extra output item (crusher and alloy smelter). */
    YIELD("yield");

    private final String name;

    UpgradeType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
