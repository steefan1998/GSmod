package nl.gsmod.stages.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import nl.gsmod.stages.StagesPayload;
import nl.gsmod.stages.compat.JeiStagesPlugin;

/** The last stage the server sent; {@code null} on servers without GS Mod: Stages, where nothing is locked. */
public final class ClientStages {
    private static final boolean JEI = ModList.get().isLoaded("jei");
    private static final boolean DAILY = ModList.get().isLoaded("gsmod_daily");

    private static @Nullable StagesPayload status;

    public static @Nullable StagesPayload status() {
        return status;
    }

    public static boolean access() {
        return status == null || status.access();
    }

    static void update(@Nullable StagesPayload payload) {
        status = payload;
        if (JEI) {
            JeiStagesPlugin.refresh();
        }
        if (DAILY) {
            DailyScreenButton.refresh(Minecraft.getInstance().screen);
        }
    }

    private ClientStages() {}
}
