package nl.gsmod.devcheck;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.gsmod.block.EnergyCellBlockEntity;
import nl.gsmod.machine.MachineBlock;
import nl.gsmod.machine.MachineBlockEntity;
import nl.gsmod.registry.ModBlocks;
import nl.gsmod.recipe.AlloyInput;
import nl.gsmod.registry.ModItems;
import nl.gsmod.registry.ModRecipes;

/**
 * Loads the test world, places the GS machines in front of the player, opens their GUIs, presses the
 * daily buttons from Inventory Buttons and takes screenshots into run-devcheck/screenshots, then quits.
 */
@Mod(value = "gsmod_devcheck", dist = Dist.CLIENT)
public final class DevCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int ticks;
    private static BlockPos generator, crusher, smelter, cell;

    public DevCheck() {
        NeoForge.EVENT_BUS.addListener(DevCheck::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        switch (ticks) {
            case 20 -> setup(mc);
            case 60 -> placeMachines(mc);
            case 150 -> mc.gui.getChat().clearMessages(false);
            case 160 -> shot(mc, "gs_1_machines_world");
            case 170 -> openMachine(mc, crusher);
            case 210 -> shot(mc, "gs_2_crusher_gui");
            case 220 -> openMachine(mc, smelter);
            case 255 -> diagnose(mc);
            case 260 -> shot(mc, "gs_3_alloy_smelter_gui");
            case 270 -> openMachine(mc, generator);
            case 310 -> shot(mc, "gs_4_generator_gui");
            case 320 -> mc.setScreen(new InventoryScreen(mc.player));
            case 360 -> shot(mc, "gs_5_inventory_buttons");
            case 370 -> press(mc, "inventorybuttons.button.missions");
            case 420 -> shot(mc, "gs_6_daily_screen");
            case 430 -> press(mc, "screen.gsmod.daily.claim");
            case 480 -> shot(mc, "gs_7_daily_claimed");
            case 490 -> mc.setScreen(null);
            case 520 -> shot(mc, "gs_8_chat_after_claim");
            case 530 -> {
                mc.gui.getChat().clearMessages(false);
                command(mc, "daily debug preview 10");
            }
            case 570 -> shot(mc, "gs_9_debug_preview_day10");
            case 580 -> {
                mc.gui.getChat().clearMessages(false);
                command(mc, "daily debug setday @s 5");
                command(mc, "daily debug info");
            }
            case 620 -> shot(mc, "gs_10_debug_info_day5");
            case 630 -> command(mc, "daily menu");
            case 670 -> shot(mc, "gs_11_daily_screen_iron_tier");
            case 690 -> mc.stop();
            default -> {
            }
        }
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    // the test world has cheats off: allow cheats, survival, daytime, clear weather
    private static void setup(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            server.getPlayerList().setAllowCommandsForAllPlayers(true);
            server.getPlayerList().getPlayers().forEach(p -> server.getPlayerList().sendPlayerPermissionLevel(p));
            var source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, "gamemode survival @a");
            server.getCommands().performPrefixedCommand(source, "time set day");
            server.getCommands().performPrefixedCommand(source, "weather clear");
            server.getCommands().performPrefixedCommand(source, "daily reset @a");
        });
    }

    private static void placeMachines(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = serverPlayer(mc);
            var level = player.level();
            Direction facing = player.getDirection();
            Direction side = facing.getClockWise();
            BlockPos base = player.blockPosition().relative(facing, 3);
            generator = base;
            crusher = base.relative(side.getOpposite());
            smelter = base.relative(side);
            cell = base.relative(side.getOpposite(), 2);
            // look straight at the machines
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), facing.toYRot(), 15);

            var front = facing.getOpposite();
            level.setBlockAndUpdate(generator, ModBlocks.COMBUSTION_GENERATOR.get().defaultBlockState().setValue(MachineBlock.FACING, front));
            level.setBlockAndUpdate(crusher, ModBlocks.CRUSHER.get().defaultBlockState().setValue(MachineBlock.FACING, front));
            level.setBlockAndUpdate(smelter, ModBlocks.ALLOY_SMELTER.get().defaultBlockState().setValue(MachineBlock.FACING, front));
            level.setBlockAndUpdate(cell, ModBlocks.ENERGY_CELL.get().defaultBlockState().setValue(MachineBlock.FACING, side));

            if (level.getBlockEntity(generator) instanceof MachineBlockEntity gen) {
                gen.setOwner(player.getUUID());
                gen.setItem(0, new ItemStack(Items.COAL, 16));
                gen.setItem(1, new ItemStack(ModItems.EFFICIENCY_UPGRADE.get(), 2));
            }
            if (level.getBlockEntity(crusher) instanceof MachineBlockEntity c) {
                c.setOwner(player.getUUID());
                c.getEnergy().generate(12_000);
                c.setItem(0, new ItemStack(ModItems.GALVANITE_ORE.get(), 12));
                c.setItem(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 2));
                c.setItem(3, new ItemStack(ModItems.YIELD_UPGRADE.get(), 1));
            }
            if (level.getBlockEntity(smelter) instanceof MachineBlockEntity s) {
                s.setOwner(player.getUUID());
                s.getEnergy().generate(9_000);
                s.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
                s.setItem(1, new ItemStack(Items.COAL, 16));
                s.setItem(3, new ItemStack(ModItems.CAPACITY_UPGRADE.get(), 3));
            }
            if (level.getBlockEntity(cell) instanceof EnergyCellBlockEntity e) {
                e.getEnergy().generate(420_000);
            }
            LOGGER.info("DevCheck placed machines at {} {} {} {}", generator, crusher, smelter, cell);
        });
    }

    private static void diagnose(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var level = serverPlayer(mc).level();
            var recipes = level.recipeAccess().recipeMap().byType(ModRecipes.ALLOYING.get());
            LOGGER.info("DevCheck alloying recipes: {}", recipes.stream().map(r -> r.id().identifier().toString()).toList());
            if (level.getBlockEntity(smelter) instanceof MachineBlockEntity s) {
                var input = new AlloyInput(s.getItem(0), s.getItem(1));
                LOGGER.info("DevCheck smelter energy={}/{} inputs={} + {} match={} state={}",
                        s.getEnergy().getEnergy(), s.getEnergy().getCapacity(), s.getItem(0), s.getItem(1),
                        level.recipeAccess().getRecipeFor(ModRecipes.ALLOYING.get(), input, level).map(r -> r.id().identifier().toString()),
                        level.getBlockState(smelter));
            }
        });
    }

    private static void openMachine(Minecraft mc, BlockPos pos) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = serverPlayer(mc);
            if (player.level().getBlockEntity(pos) instanceof MenuProvider provider) {
                player.openMenu(provider);
            } else {
                LOGGER.error("DevCheck no machine at {}", pos);
            }
        });
    }

    /** Presses the button whose label is the given translation key, like a player clicking it. */
    private static void press(Minecraft mc, String key) {
        String label = Component.translatable(key).getString();
        if (mc.screen == null) {
            LOGGER.error("DevCheck no screen open for '{}'", label);
            return;
        }
        for (GuiEventListener child : mc.screen.children()) {
            if (child instanceof Button button && button.getMessage().getString().equals(label)) {
                LOGGER.info("DevCheck press '{}' active={} on {}", label, button.active, mc.screen.getClass().getSimpleName());
                button.onPress(null);
                return;
            }
        }
        mc.screen.children().stream().filter(c -> c instanceof Button)
                .forEach(c -> LOGGER.info("DevCheck available '{}'", ((Button) c).getMessage().getString()));
        LOGGER.error("DevCheck button '{}' not found", label);
    }

    private static void command(Minecraft mc, String command) {
        LOGGER.info("DevCheck command /{}", command);
        mc.player.connection.sendCommand(command);
    }

    private static void shot(Minecraft mc, String name) {
        LOGGER.info("DevCheck screen before {}: {}", name, mc.screen == null ? null : mc.screen.getClass().getSimpleName());
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, m -> LOGGER.info("DevCheck shot {}", m.getString()));
    }
}
