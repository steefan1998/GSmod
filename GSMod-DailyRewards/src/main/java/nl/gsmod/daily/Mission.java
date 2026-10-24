package nl.gsmod.daily;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A mission definition, loaded from {@code <world>/gsmod/daily_missions/*.json}.
 *
 * <pre>
 * {
 *   "objective": "mine_block",          // see MissionObjective
 *   "target": "#c:ores/iron",           // id or #tag; depends on the objective, optional
 *   "amount": 32,
 *   "weight": 10,                       // how likely it is to be picked for a day
 *   "days": [ 5, 10 ],                  // optional: only on these days of the reward cycle
 *   "weekdays": [ "saturday", "sunday" ], // optional: only on these weekdays (of the reward day, in the configured time zone)
 *   "title": "Mine 32 iron ore",        // optional; generated from objective/target/amount otherwise
 *   "rewards": [ { "id": "minecraft:emerald", "count": 2 } ],
 *   "experience": 50
 * }
 * </pre>
 */
public record Mission(MissionObjective objective, Optional<String> target, long amount, int weight, List<Integer> days,
        List<DayOfWeek> weekdays, Optional<String> title, List<ItemStackTemplate> rewards, int experience) {

    private static final Codec<DayOfWeek> WEEKDAY_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown weekday: " + s);
        }
    }, d -> d.name().toLowerCase(Locale.ROOT));

    /** Whether this mission may be handed out on this day of the reward cycle and weekday. */
    public boolean availableOn(int cycleDay, DayOfWeek weekday) {
        return (days.isEmpty() || days.contains(cycleDay)) && (weekdays.isEmpty() || weekdays.contains(weekday));
    }

    public static final Codec<Mission> CODEC = RecordCodecBuilder.create(i -> i.group(
            MissionObjective.CODEC.fieldOf("objective").forGetter(Mission::objective),
            Codec.STRING.optionalFieldOf("target").forGetter(Mission::target),
            Codec.LONG.validate(v -> v > 0 ? DataResult.success(v)
                    : DataResult.error(() -> "amount must be positive")).fieldOf("amount").forGetter(Mission::amount),
            Codec.intRange(1, 10_000).optionalFieldOf("weight", 10).forGetter(Mission::weight),
            Codec.intRange(1, 3650).listOf().optionalFieldOf("days", List.of()).forGetter(Mission::days),
            WEEKDAY_CODEC.listOf().optionalFieldOf("weekdays", List.of()).forGetter(Mission::weekdays),
            Codec.STRING.optionalFieldOf("title").forGetter(Mission::title),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(Mission::rewards),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience", 0).forGetter(Mission::experience)
    ).apply(i, Mission::new));

    public Component displayName() {
        if (title.isPresent()) {
            return Component.translatableWithFallback(title.get(), title.get());
        }
        long shown = objective == MissionObjective.PLAY_TIME ? Math.max(1, amount / 60) : amount;
        return Component.translatable("mission.gsmod." + objective.getSerializedName(), shown, targetName());
    }

    private Component targetName() {
        String t = target.orElse("");
        if (t.isEmpty()) {
            return Component.translatable("mission.gsmod.any");
        }
        if (t.startsWith("#")) {
            return Component.literal(t);
        }
        Identifier id = Identifier.tryParse(t);
        if (id == null) {
            return Component.literal(t);
        }
        return switch (objective) {
            case MINE_BLOCK, USE_MACHINE -> BuiltInRegistries.BLOCK.getOptional(id).<Component>map(b -> b.getName()).orElse(Component.literal(t));
            default -> BuiltInRegistries.ITEM.getOptional(id).<Component>map(item -> item.getName(item.getDefaultInstance())).orElse(Component.literal(t));
        };
    }

    // --- Target matching ---

    public boolean matchesItem(ItemStack stack) {
        String t = target.orElse("");
        if (t.isEmpty()) {
            return true;
        }
        if (t.startsWith("#")) {
            Identifier tag = Identifier.tryParse(t.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(Identifier.tryParse(t) + "");
    }

    public boolean matchesBlock(BlockState state) {
        String t = target.orElse("");
        if (t.isEmpty()) {
            return true;
        }
        if (t.startsWith("#")) {
            Identifier tag = Identifier.tryParse(t.substring(1));
            return tag != null && state.is(TagKey.create(Registries.BLOCK, tag));
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(Identifier.tryParse(t) + "");
    }

    /** {@code machine} is the machine's block id, e.g. {@code gsmod:crusher}; a target without namespace matches any mod. */
    public boolean matchesMachine(Identifier machine) {
        String t = target.orElse("");
        return t.isEmpty() || t.equals(machine.toString()) || (!t.contains(":") && t.equals(machine.getPath()));
    }
}
