package nl.gsmod.daily;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import nl.gsmod.daily.test.DailyGameTests;

@Mod(GSModDaily.MODID)
public class GSModDaily {
    public static final String MODID = "gsmod_daily";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GSModDaily(IEventBus modBus, ModContainer container) {
        DailyEvents.register(modBus);
        DailyGameTests.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, DailyConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
