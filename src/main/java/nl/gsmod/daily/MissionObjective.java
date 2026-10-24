package nl.gsmod.daily;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum MissionObjective implements StringRepresentable {
    /** Break blocks; target = block id or #tag. */
    MINE_BLOCK("mine_block"),
    /** Items coming out of your own GS machines; target = item id or #tag. */
    PRODUCE_ITEM("produce_item"),
    /** Completed operations of your own GS machines (crafts, or fuel items burnt); target = machine name, empty = any. */
    USE_MACHINE("use_machine"),
    /** FE produced by your own combustion generators. */
    GENERATE_ENERGY("generate_energy"),
    /** Items crafted at a crafting grid; target = item id or #tag. */
    CRAFT_ITEM("craft_item"),
    /** Seconds spent online. */
    PLAY_TIME("play_time");

    public static final Codec<MissionObjective> CODEC = StringRepresentable.fromEnum(MissionObjective::values);

    private final String name;

    MissionObjective(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
