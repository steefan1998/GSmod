package nl.gsmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import nl.gsmod.daily.DailyEvents;
import nl.gsmod.registry.ModBlockEntities;
import nl.gsmod.registry.ModBlocks;
import nl.gsmod.registry.ModDataComponents;
import nl.gsmod.registry.ModItems;
import nl.gsmod.registry.ModMenus;
import nl.gsmod.registry.ModRecipes;
import nl.gsmod.test.GSGameTests;

@Mod(GSMod.MODID)
public class GSMod {
    public static final String MODID = "gsmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gsmod"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ModItems.CRUSHER.get().getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    public GSMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.CONDITIONS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        CREATIVE_TABS.register(modBus);

        modBus.addListener(GSMod::registerCapabilities);
        GSGameTests.register(modBus);
        DailyEvents.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Energy: works with anything that speaks NeoForge FE (Mekanism cables, other mods' generators, ...).
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CRUSHER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ALLOY_SMELTER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_CELL.get(), (be, side) -> be.getEnergy());

        // Items: pipes/hoppers insert into inputs and pull from outputs; upgrade slots stay private.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CRUSHER.get(), WorldlyContainerWrapper::new);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ALLOY_SMELTER.get(), WorldlyContainerWrapper::new);
    }
}
