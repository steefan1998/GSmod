package nl.gsmod.daily;

import java.util.Map;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import nl.gsmod.GSMod;

/** Loads reward tables from datapacks; reloaded with {@code /reload}. Server-side only. */
public class RewardTableManager extends SimpleJsonResourceReloadListener<RewardTable> {
    public static final RewardTableManager INSTANCE = new RewardTableManager();

    private volatile TreeMap<Integer, RewardTable> byDay = new TreeMap<>();

    private RewardTableManager() {
        super(RewardTable.CODEC, FileToIdConverter.json("daily_rewards"));
    }

    @Override
    protected void apply(Map<Identifier, RewardTable> tables, ResourceManager manager, ProfilerFiller profiler) {
        TreeMap<Integer, RewardTable> map = new TreeMap<>();
        tables.forEach((id, table) -> {
            RewardTable previous = map.put(table.day(), table);
            if (previous != null) {
                GSMod.LOGGER.warn("Daily reward day {} is defined more than once; using {}", table.day(), id);
            }
        });
        byDay = map;
        GSMod.LOGGER.info("Loaded {} daily reward tables (cycle of {} days)", map.size(), cycleLength());
    }

    public int cycleLength() {
        return byDay.isEmpty() ? 0 : byDay.lastKey();
    }

    /** Table for a streak (1-based), wrapping around the cycle. */
    public @Nullable RewardTable forStreak(int streak) {
        int cycle = cycleLength();
        if (cycle == 0) {
            return null;
        }
        int day = (Math.max(1, streak) - 1) % cycle + 1;
        Map.Entry<Integer, RewardTable> entry = byDay.floorEntry(day);
        return entry != null ? entry.getValue() : byDay.firstEntry().getValue();
    }
}
