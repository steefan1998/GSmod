package nl.gsmod.daily;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum MissionObjective implements StringRepresentable {
    /** Break blocks; target = block id or #tag. */
    MINE_BLOCK("mine_block"),
    /** Items produced by machines that report to Daily Rewards (e.g. GS Mod: Tech); target = item id or #tag. */
    PRODUCE_ITEM("produce_item"),
    /** Completed machine operations reported by mods (e.g. GS Mod: Tech); target = machine block id, empty = any. */
    USE_MACHINE("use_machine"),
    /** FE produced by generators that report to Daily Rewards (e.g. GS Mod: Tech). */
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
