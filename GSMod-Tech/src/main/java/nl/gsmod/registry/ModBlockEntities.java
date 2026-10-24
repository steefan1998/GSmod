package nl.gsmod.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.block.EnergyCellBlockEntity;
import nl.gsmod.machine.AlloySmelterBlockEntity;
import nl.gsmod.machine.CrusherBlockEntity;
import nl.gsmod.machine.GeneratorBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GSMod.MODID);

    public static final Supplier<BlockEntityType<GeneratorBlockEntity>> COMBUSTION_GENERATOR = BLOCK_ENTITIES.register("combustion_generator",
            () -> new BlockEntityType<>(GeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()));
    public static final Supplier<BlockEntityType<CrusherBlockEntity>> CRUSHER = BLOCK_ENTITIES.register("crusher",
            () -> new BlockEntityType<>(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()));
    public static final Supplier<BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER = BLOCK_ENTITIES.register("alloy_smelter",
            () -> new BlockEntityType<>(AlloySmelterBlockEntity::new, ModBlocks.ALLOY_SMELTER.get()));
    public static final Supplier<BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL = BLOCK_ENTITIES.register("energy_cell",
            () -> new BlockEntityType<>(EnergyCellBlockEntity::new, ModBlocks.ENERGY_CELL.get()));

    private ModBlockEntities() {}
}
