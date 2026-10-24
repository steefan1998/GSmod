package nl.gsmod.daily.api;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import nl.gsmod.daily.DailyMissions;
import nl.gsmod.daily.MissionObjective;

/**
 * Lets other mods feed the machine missions ({@code use_machine}, {@code produce_item},
 * {@code generate_energy}). Calls are ignored when the player is offline or missions are off.
 *
 * <p>Mods should only touch this class when {@code ModList.get().isLoaded("gsmod_daily")}, so Daily
 * Rewards stays optional for them.
 */
public final class DailyMissionsApi {
    /** A machine owned by {@code owner} finished one operation. {@code machine} is its block id. */
    public static void machineUsed(@Nullable MinecraftServer server, @Nullable UUID owner, Identifier machine) {
        DailyMissions.progress(server, owner, MissionObjective.USE_MACHINE, m -> m.matchesMachine(machine), 1);
    }

    /** A machine owned by {@code owner} produced {@code count} of {@code stack}. */
    public static void itemProduced(@Nullable MinecraftServer server, @Nullable UUID owner, ItemStack stack, int count) {
        DailyMissions.progress(server, owner, MissionObjective.PRODUCE_ITEM, m -> m.matchesItem(stack), count);
    }

    /** A generator owned by {@code owner} produced {@code fe} FE. */
    public static void energyGenerated(@Nullable MinecraftServer server, @Nullable UUID owner, long fe) {
        DailyMissions.progress(server, owner, MissionObjective.GENERATE_ENERGY, m -> true, fe);
    }

    private DailyMissionsApi() {}
}
