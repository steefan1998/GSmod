package nl.gsmod.daily;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * One streak day of rewards, loaded from {@code data/<namespace>/daily_rewards/*.json}.
 *
 * <pre>
 * {
 *   "day": 3,                                         // streak day this table is for (1 = first claim)
 *   "items": [ { "id": "minecraft:iron_ingot", "count": 8 } ],     // always given
 *   "random": [ { "weight": 3, "item": { "id": "minecraft:emerald" } } ],  // weighted picks
 *   "rolls": 1,                                       // how many random picks
 *   "experience": 50                                  // XP points
 * }
 * </pre>
 *
 * The highest {@code day} defines the cycle length; after it the cycle starts over. If a day has
 * no table, the closest lower day is used.
 */
public record RewardTable(int day, List<ItemStackTemplate> items, List<WeightedReward> random, int rolls, int experience) {
    public record WeightedReward(int weight, ItemStackTemplate item) {
        public static final Codec<WeightedReward> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", 1).forGetter(WeightedReward::weight),
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(WeightedReward::item)
        ).apply(i, WeightedReward::new));
    }

    public static final Codec<RewardTable> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 3650).fieldOf("day").forGetter(RewardTable::day),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(RewardTable::items),
            WeightedReward.CODEC.listOf().optionalFieldOf("random", List.of()).forGetter(RewardTable::random),
            Codec.intRange(0, 64).optionalFieldOf("rolls", 1).forGetter(RewardTable::rolls),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience", 0).forGetter(RewardTable::experience)
    ).apply(i, RewardTable::new));

    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        items.forEach(t -> result.add(t.create()));
        int totalWeight = this.random.stream().mapToInt(WeightedReward::weight).sum();
        for (int r = 0; r < rolls && totalWeight > 0; r++) {
            int pick = random.nextInt(totalWeight);
            for (WeightedReward entry : this.random) {
                pick -= entry.weight();
                if (pick < 0) {
                    result.add(entry.item().create());
                    break;
                }
            }
        }
        return result;
    }
}
