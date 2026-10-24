package nl.gsmod.daily;

import java.util.Map;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import nl.gsmod.GSMod;

/** Reward tables of the running world ({@code <world>/gsmod/daily_rewards/}). Server-side only. */
public class RewardTableManager extends WorldJsonManager<RewardTable> {
    public static final String FOLDER = "daily_rewards";
    public static final RewardTableManager INSTANCE = new RewardTableManager();

    private volatile TreeMap<Integer, RewardTable> byDay = new TreeMap<>();

    private RewardTableManager() {
        super(RewardTable.CODEC, FOLDER);
    }

    @Override
    protected void accept(Map<Identifier, RewardTable> tables, String source) {
        TreeMap<Integer, RewardTable> map = new TreeMap<>();
        tables.forEach((id, table) -> {
            RewardTable previous = map.put(table.day(), table);
            if (previous != null) {
                GSMod.LOGGER.warn("Daily reward day {} is defined more than once; using {}", table.day(), id);
            }
        });
        byDay = map;
        GSMod.LOGGER.info("Loaded {} daily reward tables (cycle of {} days) from {}", map.size(), cycleLength(), source);
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
