package nl.gsmod.daily;

import java.util.Map;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import nl.gsmod.GSMod;

/** Loads mission definitions from datapacks; reloaded with {@code /reload}. Server-side only. */
public class MissionManager extends SimpleJsonResourceReloadListener<Mission> {
    public static final MissionManager INSTANCE = new MissionManager();

    private volatile Map<Identifier, Mission> missions = Map.of();

    private MissionManager() {
        super(Mission.CODEC, FileToIdConverter.json("daily_missions"));
    }

    @Override
    protected void apply(Map<Identifier, Mission> loaded, ResourceManager manager, ProfilerFiller profiler) {
        missions = Map.copyOf(loaded);
        GSMod.LOGGER.info("Loaded {} daily missions", missions.size());
    }

    public Map<Identifier, Mission> all() {
        return missions;
    }
}
