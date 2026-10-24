package nl.gsmod.daily.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import nl.gsmod.daily.DailyStatusPayload;
import nl.gsmod.daily.GSModDaily;

@Mod(value = GSModDaily.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GSModDaily.MODID, value = Dist.CLIENT)
public class GSModDailyClient {
    public GSModDailyClient(ModContainer container) {
        // Mods screen > GS Mod: Daily Rewards > Config (the world settings, while a world is open)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(DailyStatusPayload.TYPE, (payload, context) -> DailyScreen.handle(payload));
    }
}
