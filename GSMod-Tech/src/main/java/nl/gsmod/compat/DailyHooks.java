package nl.gsmod.compat;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import nl.gsmod.daily.api.DailyMissionsApi;

/**
 * Reports machine activity to GS Mod: Daily Rewards for its machine missions. Daily Rewards is optional:
 * {@link Api} is only loaded when the mod is installed, so Tech never needs its classes otherwise.
 */
public final class DailyHooks {
    private static final boolean LOADED = ModList.get().isLoaded("gsmod_daily");

    public static void machineUsed(@Nullable MinecraftServer server, @Nullable UUID owner, Identifier machine) {
        if (LOADED && owner != null) {
            Api.machineUsed(server, owner, machine);
        }
    }

    public static void itemProduced(@Nullable MinecraftServer server, @Nullable UUID owner, ItemStack stack, int count) {
        if (LOADED && owner != null && count > 0) {
            Api.itemProduced(server, owner, stack, count);
        }
    }

    public static void energyGenerated(@Nullable MinecraftServer server, @Nullable UUID owner, long fe) {
        if (LOADED && owner != null && fe > 0) {
            Api.energyGenerated(server, owner, fe);
        }
    }

    private static final class Api {
        static void machineUsed(@Nullable MinecraftServer server, UUID owner, Identifier machine) {
            DailyMissionsApi.machineUsed(server, owner, machine);
        }

        static void itemProduced(@Nullable MinecraftServer server, UUID owner, ItemStack stack, int count) {
            DailyMissionsApi.itemProduced(server, owner, stack, count);
        }

        static void energyGenerated(@Nullable MinecraftServer server, UUID owner, long fe) {
            DailyMissionsApi.energyGenerated(server, owner, fe);
        }
    }

    private DailyHooks() {}
}
