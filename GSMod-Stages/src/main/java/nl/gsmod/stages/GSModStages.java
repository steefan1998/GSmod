package nl.gsmod.stages;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import nl.gsmod.stages.test.StagesGameTests;

@Mod(GSModStages.MODID)
public class GSModStages {
    public static final String MODID = "gsmod_stages";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GSModStages(IEventBus modBus, ModContainer container) {
        StagesEvents.register(modBus);
        StagesGameTests.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, StagesConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
