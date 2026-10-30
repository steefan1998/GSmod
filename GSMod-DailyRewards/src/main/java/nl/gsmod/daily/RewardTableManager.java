package nl.gsmod.daily;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

/** Reward tables of the running world ({@code <world>/gsmod/daily_rewards/}). Server-side only. */
public class RewardTableManager extends WorldJsonManager<RewardTable> {
    public static final String FOLDER = "daily_rewards";
    public static final RewardTableManager INSTANCE = new RewardTableManager();

    public static final String WEEK_BONUS = "week";
    public static final String COMEBACK_BONUS = "comeback";

    private volatile TreeMap<Integer, RewardTable> byDay = new TreeMap<>();
    private volatile Map<String, RewardTable> bonuses = Map.of();

    private RewardTableManager() {
        super(RewardTable.CODEC, FOLDER);
    }

    @Override
    protected void accept(Map<Identifier, RewardTable> tables, String source) {
        TreeMap<Integer, RewardTable> map = new TreeMap<>();
        Map<String, RewardTable> bonus = new HashMap<>();
        tables.forEach((id, table) -> add(map, bonus, id, table));

        // Own tables from config/gsmod_daily/custom_rewards/ go on top of the world's tables.
        if (CustomRewardsConfig.enabled()) {
            Path dir = CustomRewardsConfig.rewardsFolder();
            TreeMap<Integer, RewardTable> customDays = new TreeMap<>();
            Map<String, RewardTable> customBonus = new HashMap<>();
            readExtraFolder(dir).forEach((id, table) -> add(customDays, customBonus, id, table));
            if (!customDays.isEmpty() || !customBonus.isEmpty()) {
                if (CustomRewardsConfig.mode() == CustomRewardsConfig.Mode.REPLACE) {
                    if (customDays.isEmpty()) {
                        GSModDaily.LOGGER.warn("{} has no reward days; keeping the world's days (mode REPLACE needs at least one \"day\" file)", dir);
                    } else {
                        map.clear();
                    }
                    bonus.clear();
                }
                map.putAll(customDays);
                bonus.putAll(customBonus);
                source += " + " + dir + " (" + CustomRewardsConfig.mode() + ", days " + customDays.keySet() + ", bonuses " + customBonus.keySet() + ")";
            }
        }
        byDay = map;
        bonuses = Map.copyOf(bonus);
        GSModDaily.LOGGER.info("Loaded {} daily reward tables (cycle of {} days) and bonuses {} from {}", map.size(), cycleLength(),
                bonuses.keySet(), source);
    }

    private static void add(Map<Integer, RewardTable> days, Map<String, RewardTable> bonuses, Identifier id, RewardTable table) {
        if (table.bonus().isPresent()) {
            bonuses.put(table.bonus().get(), table);
            return;
        }
        if (table.day() < 1) {
            GSModDaily.LOGGER.warn("Daily reward table {} has neither a \"day\" nor a \"bonus\"; skipped", id);
            return;
        }
        RewardTable previous = days.put(table.day(), table);
        if (previous != null) {
            GSModDaily.LOGGER.warn("Daily reward day {} is defined more than once; using {}", table.day(), id);
        }
    }

    /** A bonus table such as {@link #WEEK_BONUS} or {@link #COMEBACK_BONUS}, or {@code null}. */
    public @Nullable RewardTable bonus(String name) {
        return bonuses.get(name);
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
