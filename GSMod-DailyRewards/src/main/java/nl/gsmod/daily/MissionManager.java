package nl.gsmod.daily;

import java.util.Map;

import net.minecraft.resources.Identifier;

/** Missions of the running world ({@code <world>/gsmod/daily_missions/}). Server-side only. */
public class MissionManager extends WorldJsonManager<Mission> {
    public static final String FOLDER = "daily_missions";
    public static final MissionManager INSTANCE = new MissionManager();

    private volatile Map<Identifier, Mission> missions = Map.of();

    private MissionManager() {
        super(Mission.CODEC, FOLDER);
    }

    @Override
    protected void accept(Map<Identifier, Mission> loaded, String source) {
        missions = Map.copyOf(loaded);
        GSModDaily.LOGGER.info("Loaded {} daily missions from {}", missions.size(), source);
    }

    public Map<Identifier, Mission> all() {
        return missions;
    }
}
