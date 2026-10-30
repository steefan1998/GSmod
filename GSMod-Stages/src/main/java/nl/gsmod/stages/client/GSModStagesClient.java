package nl.gsmod.stages.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.gsmod.stages.GSModStages;
import nl.gsmod.stages.Stages;
import nl.gsmod.stages.StagesPayload;

@Mod(value = GSModStages.MODID, dist = Dist.CLIENT)
public class GSModStagesClient {
    public GSModStagesClient(ModContainer container) {
        // Mods screen > GS Mod: Stages > Config (the world settings, while a world is open)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(GSModStagesClient::registerPayloadHandlers);
        Stages.clientAccess = ClientStages::access;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientStages.update(null));
        if (ModList.get().isLoaded("gsmod_daily")) {
            DailyScreenButton.register();
        }
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(StagesPayload.TYPE, (payload, context) -> ClientStages.update(payload));
    }
}
