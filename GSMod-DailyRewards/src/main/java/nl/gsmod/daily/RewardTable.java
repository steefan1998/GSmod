package nl.gsmod.daily;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

/**
 * One streak day of rewards, loaded from {@code <world>/gsmod/daily_rewards/*.json}.
 *
 * <pre>
 * {
 *   "day": 5,                                          // streak day this table is for (1 = first claim)
 *   "bonus": "week",                                   // instead of "day": "week" (every N days in a row) or "comeback" (streak lost)
 *   "tier": "iron",                                    // optional; shown to players (gsmod.tier.iron)
 *   "items": [ { "id": "minecraft:bread", "count": 4 } ],               // always given
 *   "random": [                                        // weighted picks, without repeats
 *     { "weight": 4, "item": { "id": "minecraft:iron_ingot" }, "min": 4, "max": 8 },
 *     { "weight": 1, "item": { "id": "minecraft:iron_pickaxe" } }
 *   ],
 *   "rolls": 2,                                        // how many random picks
 *   "experience": 50,                                  // XP points
 *   "missions": [ "gsmod:mine_iron_ore" ]             // optional: missions always given on this cycle day
 * }
 * </pre>
 *
 * The highest {@code day} defines the cycle length; after it the cycle starts over. If a day has
 * no table, the closest lower day is used.
 */
public record RewardTable(int day, Optional<String> bonus, Optional<String> tier, List<ItemStackTemplate> items, List<WeightedReward> random, int rolls,
        int experience, List<Identifier> missions) {

    /** A random pick; {@code min}/{@code max} roll the stack size, otherwise the item's own count is used. */
    public record WeightedReward(int weight, ItemStackTemplate item, Optional<Integer> min, Optional<Integer> max) {
        public static final Codec<WeightedReward> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", 1).forGetter(WeightedReward::weight),
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(WeightedReward::item),
                Codec.intRange(1, 99).optionalFieldOf("min").forGetter(WeightedReward::min),
                Codec.intRange(1, 99).optionalFieldOf("max").forGetter(WeightedReward::max)
        ).apply(i, WeightedReward::new));

        ItemStack roll(RandomSource random) {
            ItemStack stack = item.create();
            if (min.isPresent() || max.isPresent()) {
                int lo = min.orElse(max.get());
                int hi = Math.max(lo, max.orElse(lo));
                stack.setCount(Math.min(stack.getMaxStackSize(), lo + random.nextInt(hi - lo + 1)));
            }
            return stack;
        }
    }

    public static final Codec<RewardTable> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 3650).optionalFieldOf("day", 0).forGetter(RewardTable::day),
            Codec.STRING.optionalFieldOf("bonus").forGetter(RewardTable::bonus),
            Codec.STRING.optionalFieldOf("tier").forGetter(RewardTable::tier),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(RewardTable::items),
            // Entries may carry their own "neoforge:conditions", e.g. a Mekanism upgrade only while Mekanism is installed.
            ConditionalOps.decodeListWithElementConditions(WeightedReward.CODEC).optionalFieldOf("random", List.of()).forGetter(RewardTable::random),
            Codec.intRange(0, 64).optionalFieldOf("rolls", 1).forGetter(RewardTable::rolls),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience", 0).forGetter(RewardTable::experience),
            Identifier.CODEC.listOf().optionalFieldOf("missions", List.of()).forGetter(RewardTable::missions)
    ).apply(i, RewardTable::new));

    /** Display name of the tier, e.g. "Iron tier"; empty if the table has none. */
    public Component tierName() {
        return tier.<Component>map(t -> Component.translatableWithFallback("gsmod.tier." + t, t)).orElse(Component.empty());
    }

    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        items.forEach(t -> result.add(t.create()));
        // Weighted picks without repeats, so two rolls never give the same item twice.
        List<WeightedReward> pool = new ArrayList<>(this.random);
        for (int r = 0; r < rolls && !pool.isEmpty(); r++) {
            int total = pool.stream().mapToInt(WeightedReward::weight).sum();
            int pick = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                pick -= pool.get(i).weight();
                if (pick < 0) {
                    result.add(pool.remove(i).roll(random));
                    break;
                }
            }
        }
        return result;
    }
}
