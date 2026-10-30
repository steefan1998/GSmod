package nl.gsmod.stages;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Opens the stage when the dragon dies, enforces the lock, and adds {@code /stages}. */
public final class StagesEvents {
    /** What each online player was last told, so a change (unlock, game mode) is sent once. */
    private static final Map<UUID, StagesPayload> SENT = new HashMap<>();

    public static void register(IEventBus modBus) {
        Stages.ATTACHMENTS.register(modBus);
        modBus.addListener(StagesEvents::registerPayloads);
        NeoForge.EVENT_BUS.addListener(StagesEvents::registerCommands);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(StagesEvents::onCrafted);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): players without the mod can still join; they get chat messages instead of the button.
        event.registrar("1").optional().playToClient(StagesPayload.TYPE, StagesPayload.STREAM_CODEC);
    }

    // --- Opening the stage ---

    /** Worlds where the dragon was already defeated before this mod was added open right away. */
    private static void onServerStarted(ServerStartedEvent event) {
        checkDragon(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        SENT.clear();
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && dragon.level() instanceof ServerLevel level) {
            open(level.getServer());
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 == 0) {
            // Also catches a dragon fight finished some other way, e.g. by another mod.
            checkDragon(server);
        }
        if (server.getTickCount() % 20 == 0) {
            server.getPlayerList().getPlayers().forEach(StagesEvents::sync);
        }
    }

    private static void checkDragon(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        EnderDragonFight fight = end == null ? null : end.getDragonFight();
        if (fight != null && fight.hasPreviouslyKilledDragon()) {
            open(server);
        }
    }

    private static void open(MinecraftServer server) {
        if (Stages.worldOpen(server)) {
            return;
        }
        Stages.setWorldOpen(server, true);
        GSModStages.LOGGER.info("The Ender Dragon has been defeated: the end-game stage is open");
        if (StagesConfig.ANNOUNCE.get()) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.gsmod_stages.world_open").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            if (StagesConfig.REQUIRE_PLAYER_UNLOCK.get()) {
                server.getPlayerList().broadcastSystemMessage(hint(), false);
            }
        }
        server.getPlayerList().getPlayers().forEach(StagesEvents::sync);
    }

    private static Component hint() {
        return Component.translatable("message.gsmod_stages.hint").withStyle(ChatFormatting.GOLD);
    }

    /** Sends the player's stage if it changed since the last time. */
    public static void sync(ServerPlayer player) {
        StagesPayload payload = StagesPayload.of(player);
        if (!payload.equals(SENT.get(player.getUUID())) && player.connection.hasChannel(StagesPayload.TYPE)) {
            SENT.put(player.getUUID(), payload);
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SENT.remove(player.getUUID());
            sync(player);
            if (Stages.worldOpen(player.level().getServer()) && !Stages.playerUnlocked(player) && StagesConfig.REQUIRE_PLAYER_UNLOCK.get()) {
                player.sendSystemMessage(hint());
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    // --- The lock ---

    /** Checked on both sides, so the client doesn't play the use animation for something the server refuses. */
    private static boolean blocked(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return !Stages.hasAccess(serverPlayer);
        }
        return player.level().isClientSide() && !Stages.clientAccess.getAsBoolean();
    }

    private static boolean isLocked(Block block) {
        Item item = block.asItem();
        if (item != Items.AIR) {
            return Stages.isLocked(item.builtInRegistryHolder());
        }
        // Blocks without an item (e.g. multiblock parts) still count by mod.
        return StagesConfig.SPEC.isLoaded() && StagesConfig.LOCKED_MODS.get().contains(block.builtInRegistryHolder().getKey().identifier().getNamespace());
    }

    private static void tellLocked(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            boolean open = Stages.worldOpen(serverPlayer.level().getServer());
            serverPlayer.sendOverlayMessage(Component.translatable(open ? "message.gsmod_stages.locked_unlock" : "message.gsmod_stages.locked_dragon")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (Stages.isLocked(event.getItemStack()) && blocked(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            tellLocked(event.getEntity());
        }
    }

    /** Covers opening locked machines (e.g. a transmutation table) and placing locked blocks. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        boolean locked = Stages.isLocked(event.getItemStack()) || isLocked(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (locked && blocked(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            tellLocked(event.getEntity());
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Stages.isLocked(event.getItemStack()) && blocked(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            tellLocked(event.getEntity());
        }
    }

    /** A crafted locked item is taken away again; the recipe is hidden in JEI, so this is rare. */
    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (Stages.isLocked(crafted) && blocked(event.getEntity())) {
            crafted.setCount(0);
            tellLocked(event.getEntity());
        }
    }

    // --- Commands ---

    private static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("stages")
                .executes(StagesEvents::status)
                .then(Commands.literal("unlock").executes(StagesEvents::unlock))
                .then(Commands.literal("world")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("open").executes(ctx -> setWorld(ctx, true)))
                        .then(Commands.literal("close").executes(ctx -> setWorld(ctx, false))))
                .then(Commands.literal("player")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("unlock").executes(ctx -> setPlayers(ctx, true)))
                                .then(Commands.literal("lock").executes(ctx -> setPlayers(ctx, false))))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean open = Stages.worldOpen(player.level().getServer());
        player.sendSystemMessage(Component.translatable(open ? "message.gsmod_stages.status.open" : "message.gsmod_stages.status.closed")
                .withStyle(open ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        if (open && StagesConfig.REQUIRE_PLAYER_UNLOCK.get()) {
            player.sendSystemMessage(Stages.playerUnlocked(player)
                    ? Component.translatable("message.gsmod_stages.status.unlocked").withStyle(ChatFormatting.GREEN)
                    : hint());
        }
        return Stages.hasAccess(player) ? 1 : 0;
    }

    /** The one-time unlock per player (also what the button on the Daily screen runs). */
    private static int unlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!Stages.worldOpen(player.level().getServer())) {
            player.sendSystemMessage(Component.translatable("message.gsmod_stages.locked_dragon").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (Stages.playerUnlocked(player)) {
            player.sendSystemMessage(Component.translatable("message.gsmod_stages.status.unlocked").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        Stages.setPlayerUnlocked(player, true);
        player.sendSystemMessage(Component.translatable("message.gsmod_stages.unlocked").withStyle(ChatFormatting.LIGHT_PURPLE));
        sync(player);
        return 1;
    }

    private static int setWorld(CommandContext<CommandSourceStack> ctx, boolean open) {
        MinecraftServer server = ctx.getSource().getServer();
        if (open) {
            open(server);
        } else {
            Stages.setWorldOpen(server, false);
            server.getPlayerList().getPlayers().forEach(StagesEvents::sync);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(open ? "message.gsmod_stages.status.open" : "message.gsmod_stages.status.closed"), true);
        return 1;
    }

    private static int setPlayers(CommandContext<CommandSourceStack> ctx, boolean unlocked) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            Stages.setPlayerUnlocked(target, unlocked);
            sync(target);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(unlocked ? "message.gsmod_stages.players_unlocked" : "message.gsmod_stages.players_locked",
                targets.size()), true);
        return targets.size();
    }

    private StagesEvents() {}
}
