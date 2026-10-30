package nl.gsmod.stages.devcheck;

import java.lang.reflect.Field;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.gsmod.stages.StagesConfig;
import nl.gsmod.stages.client.ClientStages;

/**
 * Walks through the stage like a player, with a real end-game mod: Stellaris (put its jar and dependencies in
 * run-devcheck/mods). A Stellaris block cannot be placed and is hidden in JEI before the unlock; the Daily screen
 * button changes after the dragon and after the one-time unlock; then the block can be placed. Logs
 * "DevCheck CHECK ok/FAIL" lines and takes screenshots into run-devcheck/screenshots, then quits.
 */
@Mod(value = "gsmod_stages_devcheck", dist = Dist.CLIENT)
public final class StagesDevCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD = "stellaris";
    private static int ticks;
    private static int failures;
    private static String lastScreen = "none";
    private static BlockItem lockedBlock;
    private static BlockPos ground;

    public StagesDevCheck() {
        NeoForge.EVENT_BUS.addListener(StagesDevCheck::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        String screen = mc.screen == null ? "none" : mc.screen.getClass().getSimpleName();
        if (!screen.equals(lastScreen)) {
            LOGGER.info("DevCheck tick {}: screen {} -> {}", ticks, lastScreen, screen);
            lastScreen = screen;
        }
        switch (ticks) {
            case 20 -> setup(mc);
            case 80 -> {
                jeiRefresh();
                mc.gui.getChat().clearMessages(false);
            }
            case 90 -> {
                if (lockedBlock == null) {
                    check("a " + MOD + " block item exists (are the jars in run-devcheck/mods?)", false);
                    mc.stop();
                    return;
                }
                List<?> hidden = jeiHidden();
                check("JEI hides " + MOD + " items", !hidden.isEmpty());
                check("JEI hides only " + MOD + " items", hidden.stream().allMatch(s -> ((ItemStack) s).typeHolder().getRegisteredName().startsWith(MOD + ":")));
                check("client has no access yet", !ClientStages.access());
                place(mc);
            }
            case 120 -> {
                check("locked " + MOD + " block not placed", !placed(mc));
                shot(mc, "stages_1_locked_" + MOD);
            }
            case 130 -> command(mc, "daily menu");
            case 170 -> {
                logButtons(mc);
                Button locked = findButton(mc, "screen.gsmod_stages.locked");
                check("button says defeat the dragon", locked != null && !locked.active);
                shot(mc, "stages_2_daily_before_dragon");
            }
            case 180 -> {
                mc.setScreen(null);
                mc.gui.getChat().clearMessages(false);
                command(mc, "stages world open");
            }
            case 220 -> shot(mc, "stages_3_dragon_defeated_chat");
            case 230 -> command(mc, "daily menu");
            case 270 -> {
                logButtons(mc);
                Button unlock = findButton(mc, "screen.gsmod_stages.unlock");
                check("unlock button active after the dragon", unlock != null && unlock.active);
                check("still locked before pressing", !ClientStages.access() && !jeiHidden().isEmpty());
                shot(mc, "stages_4_daily_unlock_button");
                if (unlock != null) {
                    unlock.onPress(null);
                }
            }
            case 320 -> {
                logButtons(mc);
                Button done = findButton(mc, "screen.gsmod_stages.unlocked");
                check("button shows unlocked", done != null && !done.active);
                check("client has access", ClientStages.access());
                check("JEI shows the " + MOD + " items again", jeiHidden().isEmpty());
                shot(mc, "stages_5_daily_unlocked");
            }
            case 330 -> {
                mc.setScreen(null);
                place(mc);
            }
            case 360 -> {
                check(MOD + " block can be placed now", placed(mc));
                shot(mc, "stages_6_unlocked_" + MOD);
            }
            case 370 -> command(mc, "stages unlock");
            case 400 -> {
                shot(mc, "stages_7_unlock_again_chat");
                LOGGER.info("DevCheck DONE with {} failure(s)", failures);
            }
            case 410 -> mc.stop();
            default -> {
            }
        }
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    /** Cheats on, survival, a fresh locked stage, only the default locked mods, and a Stellaris block in hand. */
    private static void setup(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            StagesConfig.LOCKED_ITEMS.set(List.of());
            LOGGER.info("DevCheck lockedMods {}", StagesConfig.LOCKED_MODS.get());
            server.getPlayerList().setAllowCommandsForAllPlayers(true);
            server.getPlayerList().getPlayers().forEach(p -> server.getPlayerList().sendPlayerPermissionLevel(p));
            var source = server.createCommandSourceStack();
            for (String command : List.of("gamemode survival @a", "time set day", "weather clear", "stages world close",
                    "stages player @a lock", "clear @a")) {
                server.getCommands().performPrefixedCommand(source, command);
            }
            // A plain full block from the mod, so placing it is a fair test.
            List<BlockItem> blocks = BuiltInRegistries.ITEM.stream()
                    .filter(item -> item instanceof BlockItem && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MOD))
                    .map(item -> (BlockItem) item).toList();
            lockedBlock = blocks.stream().filter(b -> BuiltInRegistries.ITEM.getKey(b).getPath().endsWith("_block")).findFirst()
                    .orElse(blocks.isEmpty() ? null : blocks.get(0));
            LOGGER.info("DevCheck {} block items: {}, using {}", MOD, blocks.size(), lockedBlock);
            ServerPlayer player = serverPlayer(mc);
            if (lockedBlock != null) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(lockedBlock, 4));
            }
            Direction facing = player.getDirection();
            ground = player.blockPosition().relative(facing, 2).below();
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), facing.toYRot(), 35);
            LOGGER.info("DevCheck setup done, placing on {}", ground);
        });
    }

    /** Right-click the top of the ground two blocks ahead with the block in hand. */
    private static void place(Minecraft mc) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
    }

    private static boolean placed(Minecraft mc) {
        ServerPlayer player = serverPlayer(mc);
        // Grass or flowers on the ground get replaced, so the block can end up in the ground position itself.
        var below = player.level().getBlockState(ground);
        var above = player.level().getBlockState(ground.above());
        LOGGER.info("DevCheck ground: {}, above: {}, in hand: {}", below, above, player.getMainHandItem());
        return below.is(lockedBlock.getBlock()) || above.is(lockedBlock.getBlock());
    }

    // JEI is only on the runtime classpath, so the plugin is reached by reflection.
    private static void jeiRefresh() {
        try {
            Class.forName("nl.gsmod.stages.compat.JeiStagesPlugin").getMethod("refresh").invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("DevCheck JEI refresh failed", e);
        }
    }

    private static List<?> jeiHidden() {
        try {
            Field field = Class.forName("nl.gsmod.stages.compat.JeiStagesPlugin").getDeclaredField("hidden");
            field.setAccessible(true);
            List<?> hidden = (List<?>) field.get(null);
            LOGGER.info("DevCheck JEI hidden: {} items, first {}", hidden.size(), hidden.stream().limit(5).toList());
            return hidden;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("DevCheck JEI check failed", e);
            return List.of();
        }
    }

    private static Button findButton(Minecraft mc, String key) {
        if (mc.screen == null) {
            return null;
        }
        String label = Component.translatable(key).getString();
        for (GuiEventListener child : mc.screen.children()) {
            if (child instanceof Button button && button.getMessage().getString().equals(label)) {
                return button;
            }
        }
        return null;
    }

    private static void logButtons(Minecraft mc) {
        if (mc.screen != null) {
            mc.screen.children().stream().filter(c -> c instanceof Button)
                    .forEach(c -> LOGGER.info("DevCheck button '{}' active={}", ((Button) c).getMessage().getString(), ((Button) c).active));
        }
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        LOGGER.info("DevCheck CHECK {} - {}", ok ? "ok" : "FAIL", what);
    }

    private static void command(Minecraft mc, String command) {
        LOGGER.info("DevCheck command /{}", command);
        mc.player.connection.sendCommand(command);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, m -> LOGGER.info("DevCheck shot {}", m.getString()));
    }
}
