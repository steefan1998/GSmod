package nl.gsmod.daily;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import nl.gsmod.ServerConfig;
import nl.gsmod.GSMod;

/** Wires daily rewards and missions into the game: commands, datapack loading, login, and progress events. */
public final class DailyEvents {
    public static void register(IEventBus modBus) {
        DailyRewards.ATTACHMENTS.register(modBus);
        modBus.addListener(DailyEvents::registerPayloads);
        NeoForge.EVENT_BUS.addListener(DailyEvents::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(DailyEvents::registerCommands);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onCrafted);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onServerStarting);
        NeoForge.EVENT_BUS.addListener(DailyEvents::onServerStopped);
    }

    /** Switch reward tables and missions to this world's own files in {@code <world>/gsmod/}. */
    private static void onServerStarting(ServerStartingEvent event) {
        Path world = event.getServer().getWorldPath(LevelResource.ROOT).normalize();
        WorldJsonManager.setWorld(world);
        RewardTableManager.INSTANCE.refresh();
        MissionManager.INSTANCE.refresh();
        writeWorldExtras(world);
    }

    /**
     * NeoForge keeps server configs in the global config folder; a copy in {@code <world>/serverconfig/}
     * overrides it for that world. Put that copy in place, so each world's daily settings sit next to its
     * reward and mission files.
     */
    private static void writeWorldExtras(Path world) {
        try {
            Path worldConfig = world.resolve("serverconfig").resolve(GSMod.MODID + "-server.toml");
            Path globalConfig = FMLPaths.CONFIGDIR.get().resolve(GSMod.MODID + "-server.toml");
            if (!Files.exists(worldConfig) && Files.isRegularFile(globalConfig)) {
                Files.createDirectories(worldConfig.getParent());
                Files.copy(globalConfig, worldConfig);
                GSMod.LOGGER.info("Created {} (edit it to change this world's daily settings; applies on the next world start)", worldConfig);
            }
            Path readme = world.resolve(GSMod.MODID).resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.createDirectories(readme.getParent());
                Files.writeString(readme, WORLD_README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            GSMod.LOGGER.warn("Could not write GS Mod world files to {}", world, e);
        }
    }

    private static final String WORLD_README = """
            GS Mod - daily rewards and missions for this world
            ==================================================

            daily_rewards/   One JSON file per streak day. The highest "day" is the cycle length.
                             "tier" is shown to players (iron, diamond, netherite, ...).
                             "items" are always given, "rolls" picks from "random" by "weight",
                             "min"/"max" give a random stack size.
            daily_missions/  One JSON file per mission. Each day every player gets a few of them,
                             picked by "weight". Files with "neoforge:conditions" only count while
                             that mod is installed.

            Delete a file to remove it, add files to add rewards or missions. Changes apply with
            /reload or on the next world start. Delete a whole folder to get the defaults back.

            ../serverconfig/gsmod-server.toml holds this world's settings: rewards on/off, reset hour
            (UTC), streak grace days, missions on/off and missions per day. Changes there apply on the
            next world start.
            """;

    private static void onServerStopped(ServerStoppedEvent event) {
        WorldJsonManager.setWorld(null);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): vanilla clients and clients without GS Mod can still join; they just get chat output.
        event.registrar("1").optional().playToClient(DailyStatusPayload.TYPE, DailyStatusPayload.STREAM_CODEC);
    }

    private static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(GSMod.id("daily_rewards"), RewardTableManager.INSTANCE);
        event.addListener(GSMod.id("daily_missions"), MissionManager.INSTANCE);
    }

    // --- Commands ---

    private static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("daily")
                .executes(DailyEvents::claim)
                .then(Commands.literal("claim").executes(DailyEvents::claim))
                .then(Commands.literal("status").executes(DailyEvents::status))
                .then(Commands.literal("missions").executes(DailyEvents::missions))
                .then(Commands.literal("menu").executes(DailyEvents::menu))
                .then(DailyDebugCommands.build())
                .then(Commands.literal("reset")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(DailyEvents::reset))));
    }

    private static int claim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Instant now = Instant.now();
        DailyRewards.ClaimResult result = DailyRewards.claim(player, now);
        DailyRewards.sendResult(player, result, now);
        refreshScreen(player, now);
        return result.status() == DailyRewards.Status.CLAIMED ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Instant now = Instant.now();
        DailyStatusPayload status = DailyStatusPayload.of(player, now);
        if (status.canClaim()) {
            player.sendSystemMessage(Component.translatable("message.gsmod.daily.available", status.nextStreak()).withStyle(ChatFormatting.GREEN));
            if (!status.nextTier().getString().isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.gsmod.daily.tier", status.nextTier()).withStyle(ChatFormatting.GOLD));
            }
        } else {
            player.sendSystemMessage(Component.translatable("message.gsmod.daily.already",
                    DailyClock.formatDuration(status.secondsUntilReset())).withStyle(ChatFormatting.YELLOW));
        }
        player.sendSystemMessage(Component.translatable("message.gsmod.daily.streak", status.streak()).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int missions(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DailyStatusPayload status = DailyStatusPayload.of(player, Instant.now());
        if (!DailyMissions.enabled() || status.missions().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.gsmod.mission.none").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("message.gsmod.mission.header",
                DailyClock.formatDuration(status.secondsUntilReset())).withStyle(ChatFormatting.GOLD));
        for (DailyStatusPayload.Line line : status.missions()) {
            player.sendSystemMessage(Component.literal(line.completed() ? " ✔ " : " • ").append(line.title())
                    .append(Component.literal("  " + line.progress() + "/" + line.amount()))
                    .withStyle(line.completed() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        return 1;
    }

    /** Opens the Daily screen on clients that have GS Mod; falls back to chat for the rest. */
    private static int menu(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (player.connection.hasChannel(DailyStatusPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, DailyStatusPayload.of(player, Instant.now()));
            return 1;
        }
        status(ctx);
        return missions(ctx);
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            target.setData(DailyRewards.DATA, DailyRewardData.EMPTY);
            target.setData(DailyMissions.DATA, DailyMissionData.EMPTY);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.gsmod.daily.reset", targets.size()), true);
        return targets.size();
    }

    private static void refreshScreen(ServerPlayer player, Instant now) {
        if (player.connection.hasChannel(DailyStatusPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, DailyStatusPayload.of(player, now));
        }
    }

    // --- Events ---

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.DAILY_NOTIFY_ON_LOGIN.get()
                && DailyRewards.canClaim(player, Instant.now()) && RewardTableManager.INSTANCE.cycleLength() > 0) {
            player.sendSystemMessage(Component.translatable("message.gsmod.daily.login").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player) {
            DailyMissions.progress(player, MissionObjective.MINE_BLOCK, m -> m.matchesBlock(event.getState()), 1);
        }
    }

    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var crafted = event.getCrafting();
            DailyMissions.progress(player, MissionObjective.CRAFT_ITEM, m -> m.matchesItem(crafted), crafted.getCount());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            DailyMissions.progress(player, MissionObjective.PLAY_TIME, m -> true, 1);
        }
    }

    private DailyEvents() {}
}
