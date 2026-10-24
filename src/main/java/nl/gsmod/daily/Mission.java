package nl.gsmod.daily;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import nl.gsmod.machine.MachineType;

/**
 * A mission definition, loaded from {@code data/<namespace>/daily_missions/*.json}.
 *
 * <pre>
 * {
 *   "objective": "mine_block",          // see MissionObjective
 *   "target": "#c:ores/iron",           // id or #tag; depends on the objective, optional
 *   "amount": 32,
 *   "weight": 10,                       // how likely it is to be picked for a day
 *   "title": "Mine 32 iron ore",        // optional; generated from objective/target/amount otherwise
 *   "rewards": [ { "id": "minecraft:emerald", "count": 2 } ],
 *   "experience": 50
 * }
 * </pre>
 */
public record Mission(MissionObjective objective, Optional<String> target, long amount, int weight, Optional<String> title,
        List<ItemStackTemplate> rewards, int experience) {

    public static final Codec<Mission> CODEC = RecordCodecBuilder.create(i -> i.group(
            MissionObjective.CODEC.fieldOf("objective").forGetter(Mission::objective),
            Codec.STRING.optionalFieldOf("target").forGetter(Mission::target),
            Codec.LONG.validate(v -> v > 0 ? com.mojang.serialization.DataResult.success(v)
                    : com.mojang.serialization.DataResult.error(() -> "amount must be positive")).fieldOf("amount").forGetter(Mission::amount),
            Codec.intRange(1, 10_000).optionalFieldOf("weight", 10).forGetter(Mission::weight),
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
            case MINE_BLOCK -> BuiltInRegistries.BLOCK.getOptional(id).<Component>map(b -> b.getName()).orElse(Component.literal(t));
            case USE_MACHINE -> Component.translatable("block.gsmod." + machineBlockName(t));
            default -> BuiltInRegistries.ITEM.getOptional(id).<Component>map(item -> item.getName(item.getDefaultInstance())).orElse(Component.literal(t));
        };
    }

    private static String machineBlockName(String machine) {
        return machine.equals(MachineType.GENERATOR.getSerializedName()) ? "combustion_generator" : machine;
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

    public boolean matchesMachine(MachineType type) {
        String t = target.orElse("");
        return t.isEmpty() || t.equals(type.getSerializedName()) || t.equals(machineBlockName(type.getSerializedName()))
                || t.equals("gsmod:" + machineBlockName(type.getSerializedName()));
    }
}
