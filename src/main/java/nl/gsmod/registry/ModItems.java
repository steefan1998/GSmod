package nl.gsmod.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gsmod.GSMod;
import nl.gsmod.block.EnergyCellItem;
import nl.gsmod.item.UpgradeItem;
import nl.gsmod.item.UpgradeType;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GSMod.MODID);

    // --- Galvanite ---
    public static final DeferredItem<Item> RAW_GALVANITE = ITEMS.registerSimpleItem("raw_galvanite");
    public static final DeferredItem<Item> GALVANITE_DUST = ITEMS.registerSimpleItem("galvanite_dust");
    public static final DeferredItem<Item> GALVANITE_INGOT = ITEMS.registerSimpleItem("galvanite_ingot");
    public static final DeferredItem<BlockItem> GALVANITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.GALVANITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_GALVANITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_GALVANITE_ORE);
    public static final DeferredItem<BlockItem> GALVANITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GALVANITE_BLOCK);

    // --- Steel ---
    public static final DeferredItem<Item> STEEL_DUST = ITEMS.registerSimpleItem("steel_dust");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<BlockItem> STEEL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_BLOCK);

    // --- Crafting components ---
    public static final DeferredItem<Item> MACHINE_FRAME = ITEMS.registerSimpleItem("machine_frame");
    public static final DeferredItem<Item> GALVANITE_COIL = ITEMS.registerSimpleItem("galvanite_coil");

    // --- Machines ---
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> CRUSHER = ITEMS.registerSimpleBlockItem(ModBlocks.CRUSHER);
    public static final DeferredItem<BlockItem> ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.ALLOY_SMELTER);
    public static final DeferredItem<EnergyCellItem> ENERGY_CELL = ITEMS.registerItem("energy_cell",
            p -> new EnergyCellItem(ModBlocks.ENERGY_CELL.get(), p), p -> p.useBlockDescriptionPrefix().stacksTo(1));

    // --- Upgrades ---
    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE = upgrade(UpgradeType.SPEED);
    public static final DeferredItem<UpgradeItem> EFFICIENCY_UPGRADE = upgrade(UpgradeType.EFFICIENCY);
    public static final DeferredItem<UpgradeItem> CAPACITY_UPGRADE = upgrade(UpgradeType.CAPACITY);
    public static final DeferredItem<UpgradeItem> YIELD_UPGRADE = upgrade(UpgradeType.YIELD);

    private static DeferredItem<UpgradeItem> upgrade(UpgradeType type) {
        return ITEMS.registerItem(type.getSerializedName() + "_upgrade", p -> new UpgradeItem(type, p), p -> p.stacksTo(16));
    }

    private ModItems() {}
}
