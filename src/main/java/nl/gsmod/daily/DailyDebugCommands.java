package nl.gsmod.daily;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.gsmod.ServerConfig;

/**
 * {@code /daily debug ...} for operators: inspect and change daily reward and mission state, preview
 * reward days and reload this world's files. Output is plain English, as it is meant for admins.
 */
final class DailyDebugCommands {
    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("debug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> info(ctx, EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("setday")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, 3650)).executes(DailyDebugCommands::setDay))))
                .then(Commands.literal("unclaim")
                        .then(Commands.argument("targets", EntityArgument.players()).executes(DailyDebugCommands::unclaim)))
                .then(Commands.literal("preview")
                        .then(Commands.argument("day", IntegerArgumentType.integer(1, 3650)).executes(DailyDebugCommands::preview)))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, 3650)).executes(DailyDebugCommands::give))))
                .then(Commands.literal("missions")
                        .then(Commands.literal("list").executes(DailyDebugCommands::listMissions))
                        .then(Commands.literal("reroll")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(DailyDebugCommands::reroll)))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> complete(ctx, -1))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1, 20))
                                                .executes(ctx -> complete(ctx, IntegerArgumentType.getInteger(ctx, "index") - 1)))))
                        .then(Commands.literal("progress")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1, 20))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(DailyDebugCommands::progress))))))
                .then(Commands.literal("reload").executes(DailyDebugCommands::reload));
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> message, false);
    }

    private static int info(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        Instant now = Instant.now();
        long today = DailyRewards.today(now);
        for (ServerPlayer player : targets) {
            DailyRewardData data = DailyRewards.data(player);
            int cycleDay = DailyMissions.cycleDay(player, today);
            RewardTable table = RewardTableManager.INSTANCE.forStreak(cycleDay);
            reply(ctx, Component.literal("== " + player.getGameProfile().name() + " ==").withStyle(ChatFormatting.GOLD));
            reply(ctx, Component.literal("today=" + today + " (" + DailyMissions.weekday(today).name().toLowerCase() + "), lastClaimDay="
                    + (data.lastClaimDay() == DailyRewardData.NEVER ? "never" : data.lastClaimDay())
                    + ", streak=" + data.streak() + ", totalClaims=" + data.totalClaims()));
            reply(ctx, Component.literal("canClaim=" + data.canClaim(today) + ", cycleDay=" + cycleDay + "/" + RewardTableManager.INSTANCE.cycleLength()
                    + ", tier=" + (table == null ? "-" : table.tier().orElse("-"))
                    + ", resetIn=" + DailyClock.formatDuration(DailyClock.secondsUntilReset(now, ServerConfig.DAILY_RESET_HOUR_UTC.get()))));
            List<DailyMissionData.Entry> missions = DailyMissions.current(player, now).missions();
            for (int i = 0; i < missions.size(); i++) {
                DailyMissionData.Entry e = missions.get(i);
                Mission m = MissionManager.INSTANCE.all().get(e.mission());
                reply(ctx, Component.literal(" " + (i + 1) + ". " + e.mission() + "  " + e.progress() + "/" + (m == null ? "?" : m.amount())
                        + (e.completed() ? "  done" : "")).withStyle(e.completed() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        return targets.size();
    }

    /** The next claim gives reward day {@code day}; today's missions are re-rolled for that day. */
    private static int setDay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int day = IntegerArgumentType.getInteger(ctx, "day");
        long today = DailyRewards.today(Instant.now());
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            DailyRewardData old = DailyRewards.data(player);
            player.setData(DailyRewards.DATA, day <= 1
                    ? new DailyRewardData(DailyRewardData.NEVER, 0, old.totalClaims())
                    : new DailyRewardData(today - 1, day - 1, old.totalClaims()));
            player.setData(DailyMissions.DATA, DailyMissions.roll(player, today, 0));
        }
        reply(ctx, Component.literal("Next claim gives day " + day + " for " + targets.size() + " player(s)"));
        return targets.size();
    }

    /** Lets players claim again today, keeping their streak. */
    private static int unclaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        long today = DailyRewards.today(Instant.now());
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            DailyRewardData old = DailyRewards.data(player);
            if (old.lastClaimDay() != DailyRewardData.NEVER && old.lastClaimDay() >= today) {
                player.setData(DailyRewards.DATA, new DailyRewardData(today - 1, old.streak(), old.totalClaims()));
            }
        }
        reply(ctx, Component.literal("Today's claim reset for " + targets.size() + " player(s)"));
        return targets.size();
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) {
        int day = IntegerArgumentType.getInteger(ctx, "day");
        RewardTable table = RewardTableManager.INSTANCE.forStreak(day);
        if (table == null) {
            ctx.getSource().sendFailure(Component.literal("No reward tables loaded"));
            return 0;
        }
        reply(ctx, Component.literal("Day " + day + " -> table day " + table.day() + (table.tier().map(t -> " (" + t + ")").orElse(""))
                + ", rolls=" + table.rolls() + ", xp=" + table.experience()).withStyle(ChatFormatting.GOLD));
        table.items().forEach(t -> reply(ctx, Component.literal(" always: ").append(describe(t.create()))));
        int total = table.random().stream().mapToInt(RewardTable.WeightedReward::weight).sum();
        for (RewardTable.WeightedReward r : table.random()) {
            String count = r.min().isPresent() || r.max().isPresent()
                    ? r.min().orElse(r.max().orElse(1)) + "-" + r.max().orElse(r.min().orElse(1)) + "x "
                    : r.item().count() + "x ";
            reply(ctx, Component.literal(String.format(" %4.1f%% ", 100.0 * r.weight() / total) + count)
                    .append(r.item().create().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (!table.missions().isEmpty()) {
            reply(ctx, Component.literal(" missions: " + table.missions()));
        }
        reply(ctx, Component.literal(" example roll:").withStyle(ChatFormatting.AQUA));
        table.roll(ctx.getSource().getLevel().getRandom()).forEach(s -> reply(ctx, Component.literal("  ").append(describe(s))));
        return 1;
    }

    /** Hands out a reward day without touching streaks. */
    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int day = IntegerArgumentType.getInteger(ctx, "day");
        RewardTable table = RewardTableManager.INSTANCE.forStreak(day);
        if (table == null) {
            ctx.getSource().sendFailure(Component.literal("No reward tables loaded"));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            List<ItemStack> items = table.roll(player.getRandom());
            Rewards.give(player, items, table.experience());
            MutableComponent line = Component.literal("Gave day " + day + " to " + player.getGameProfile().name() + ":");
            items.forEach(s -> line.append(" ").append(describe(s)));
            reply(ctx, line);
        }
        return targets.size();
    }

    private static int listMissions(CommandContext<CommandSourceStack> ctx) {
        Map<Identifier, Mission> all = MissionManager.INSTANCE.all();
        reply(ctx, Component.literal(all.size() + " missions loaded").withStyle(ChatFormatting.GOLD));
        all.entrySet().stream().sorted(Map.Entry.comparingByKey((a, b) -> a.toString().compareTo(b.toString()))).forEach(e -> {
            Mission m = e.getValue();
            String when = (m.days().isEmpty() ? "" : " days=" + m.days()) + (m.weekdays().isEmpty() ? "" : " weekdays=" + m.weekdays());
            reply(ctx, Component.literal(" " + e.getKey() + " [" + m.objective().getSerializedName() + " " + m.target().orElse("*") + " x" + m.amount()
                    + ", weight " + m.weight() + when + "]").withStyle(ChatFormatting.GRAY));
        });
        return all.size();
    }

    private static int reroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        long today = DailyRewards.today(Instant.now());
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            player.setData(DailyMissions.DATA, DailyMissions.roll(player, today, player.getRandom().nextLong()));
        }
        reply(ctx, Component.literal("Re-rolled missions for " + targets.size() + " player(s)"));
        return targets.size();
    }

    /** Completes one mission (1-based index) or, without index, all of today's missions; rewards are paid. */
    private static int complete(CommandContext<CommandSourceStack> ctx, int index) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int done = 0;
        for (ServerPlayer player : targets) {
            int size = DailyMissions.current(player, Instant.now()).missions().size();
            for (int i = index < 0 ? 0 : index; i < (index < 0 ? size : index + 1); i++) {
                if (DailyMissions.progressAt(player, i, 0)) {
                    done++;
                }
            }
        }
        reply(ctx, Component.literal("Completed " + done + " mission(s)"));
        return done;
    }

    private static int progress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (DailyMissions.progressAt(player, index, amount)) {
                changed++;
            }
        }
        reply(ctx, Component.literal("Added " + amount + " progress to mission " + (index + 1) + " for " + changed + " player(s)"));
        return changed;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        RewardTableManager.INSTANCE.refresh();
        MissionManager.INSTANCE.refresh();
        reply(ctx, Component.literal("Reloaded " + RewardTableManager.INSTANCE.cycleLength() + " reward days and "
                + MissionManager.INSTANCE.all().size() + " missions from this world's gsmod folder"));
        return 1;
    }

    private static Component describe(ItemStack stack) {
        return Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
    }

    private DailyDebugCommands() {}
}
