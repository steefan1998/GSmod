package nl.gsmod.stages;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The end-game stage. It opens for the whole world once the Ender Dragon is defeated (stored on the overworld),
 * and then each player unlocks it once for themselves (stored on the player). Until both are true, items from
 * {@link StagesConfig#LOCKED_MODS} and {@link StagesConfig#LOCKED_ITEMS} are locked for that player.
 */
public final class Stages {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GSModStages.MODID);

    /** On the overworld: the dragon has been defeated in this world. */
    public static final Supplier<AttachmentType<Boolean>> WORLD_OPEN = ATTACHMENTS.register("endgame_open",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("open")).build());
    /** On a player: they unlocked the stage for themselves. Kept on death, never turned off by the player. */
    public static final Supplier<AttachmentType<Boolean>> PLAYER_UNLOCKED = ATTACHMENTS.register("endgame_unlocked",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("unlocked")).copyOnDeath().build());

    public static boolean worldOpen(MinecraftServer server) {
        return server.overworld().getData(WORLD_OPEN);
    }

    public static void setWorldOpen(MinecraftServer server, boolean open) {
        server.overworld().setData(WORLD_OPEN, open);
    }

    public static boolean playerUnlocked(ServerPlayer player) {
        return player.getData(PLAYER_UNLOCKED);
    }

    public static void setPlayerUnlocked(ServerPlayer player, boolean unlocked) {
        player.setData(PLAYER_UNLOCKED, unlocked);
    }

    /** Whether the player may use locked content now. */
    public static boolean hasAccess(ServerPlayer player) {
        return access(worldOpen(player.level().getServer()), playerUnlocked(player)) || bypass(player);
    }

    /** The rule without the creative bypass. */
    public static boolean access(boolean worldOpen, boolean playerUnlocked) {
        return worldOpen && (playerUnlocked || !StagesConfig.REQUIRE_PLAYER_UNLOCK.get());
    }

    /** The local player's access as the server last sent it; set by the client half of the mod. */
    public static BooleanSupplier clientAccess = () -> true;

    public static boolean bypass(Player player) {
        return StagesConfig.CREATIVE_BYPASS.get() && (player.isCreative() || player.isSpectator());
    }

    // --- Which items are locked ---

    public static boolean isLocked(ItemStack stack) {
        return !stack.isEmpty() && isLocked(stack.typeHolder());
    }

    /** Uses the config, which the server also sends to clients; false while no world is loaded. */
    public static boolean isLocked(Holder<Item> item) {
        return StagesConfig.SPEC.isLoaded()
                && matches(item, StagesConfig.LOCKED_MODS.get(), StagesConfig.LOCKED_ITEMS.get(), StagesConfig.ALLOWED_ITEMS.get());
    }

    /** {@code item} is from one of {@code mods} or listed in {@code items}, and not listed in {@code allowed}. */
    public static boolean matches(Holder<Item> item, Collection<? extends String> mods, Collection<? extends String> items, Collection<? extends String> allowed) {
        Identifier id = item.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (id == null) {
            return false;
        }
        boolean locked = mods.contains(id.getNamespace()) || anyMatch(item, id, items);
        return locked && !anyMatch(item, id, allowed);
    }

    private static boolean anyMatch(Holder<Item> item, Identifier id, Collection<? extends String> entries) {
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && item.is(TagKey.create(Registries.ITEM, tag))) {
                    return true;
                }
            } else if (id.toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private Stages() {}
}
