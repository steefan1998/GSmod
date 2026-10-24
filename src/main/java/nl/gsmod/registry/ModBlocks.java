package nl.gsmod.registry;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.block.EnergyCellBlock;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.machine.MachineType;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GSMod.MODID);

    // --- Materials ---
    public static final DeferredBlock<DropExperienceBlock> GALVANITE_ORE = BLOCKS.registerBlock("galvanite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            p -> p.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_GALVANITE_ORE = BLOCKS.registerBlock("deepslate_galvanite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            p -> p.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> GALVANITE_BLOCK = BLOCKS.registerSimpleBlock("galvanite_block",
            p -> p.mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.registerSimpleBlock("steel_block",
            p -> p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));

    // --- Machines ---
    public static final DeferredBlock<MachineBlock> COMBUSTION_GENERATOR = machine("combustion_generator", MachineType.GENERATOR);
    public static final DeferredBlock<MachineBlock> CRUSHER = machine("crusher", MachineType.CRUSHER);
    public static final DeferredBlock<MachineBlock> ALLOY_SMELTER = machine("alloy_smelter", MachineType.ALLOY_SMELTER);

    // --- Energy storage ---
    public static final DeferredBlock<EnergyCellBlock> ENERGY_CELL = BLOCKS.registerBlock("energy_cell",
            EnergyCellBlock::new, ModBlocks::machineProperties);

    private static DeferredBlock<MachineBlock> machine(String name, MachineType type) {
        return BLOCKS.registerBlock(name, p -> new MachineBlock(p, type), ModBlocks::machineProperties);
    }

    private static BlockBehaviour.Properties machineProperties(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL);
    }

    private ModBlocks() {}
}
