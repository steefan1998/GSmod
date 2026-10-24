package nl.gsmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.gsmod.client.MachineScreen;
import nl.gsmod.registry.ModMenus;

@Mod(value = GSMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GSMod.MODID, value = Dist.CLIENT)
public class GSModClient {
    public GSModClient(ModContainer container) {
        // Mods screen > GS Mod > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), MachineScreen::new);
        event.register(ModMenus.CRUSHER.get(), MachineScreen::new);
        event.register(ModMenus.ALLOY_SMELTER.get(), MachineScreen::new);
    }
}
