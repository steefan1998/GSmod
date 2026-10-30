package nl.gsmod.stages.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.gsmod.daily.client.DailyScreen;
import nl.gsmod.stages.StagesPayload;

/**
 * The end-game button under the Daily screen of GS Mod: Daily Rewards. It only sends {@code /stages unlock};
 * the server checks it and answers with a new {@link StagesPayload}. Only loaded when gsmod_daily is installed.
 */
final class DailyScreenButton {
    static void register() {
        NeoForge.EVENT_BUS.addListener(DailyScreenButton::onInit);
    }

    private static void onInit(ScreenEvent.Init.Post event) {
        StagesPayload status = ClientStages.status();
        if (!(event.getScreen() instanceof DailyScreen screen) || status == null) {
            return;
        }
        boolean canUnlock = status.worldOpen() && status.requireUnlock() && !status.unlocked();
        String key = !status.worldOpen() ? "screen.gsmod_stages.locked" : canUnlock ? "screen.gsmod_stages.unlock" : "screen.gsmod_stages.unlocked";
        Button button = Button.builder(Component.translatable(key), b -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null) {
                        minecraft.player.connection.sendCommand("stages unlock");
                        b.active = false;
                    }
                })
                .bounds(screen.panelLeft(), screen.panelBottom() + 4, screen.panelWidth(), 20)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build();
        button.active = canUnlock;
        event.addListener(button);
    }

    /** Rebuild an open Daily screen so the button shows the new stage. */
    static void refresh(@Nullable Screen screen) {
        if (screen instanceof DailyScreen daily) {
            daily.refreshWidgets();
        }
    }

    private DailyScreenButton() {}
}
