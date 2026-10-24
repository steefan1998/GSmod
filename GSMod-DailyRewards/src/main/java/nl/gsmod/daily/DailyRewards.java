package nl.gsmod.daily;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Daily login reward. Everything runs on the server: the client only sends the {@code /daily}
 * command, and the server decides from its own clock and the stored player data whether a reward is due.
 */
public final class DailyRewards {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GSModDaily.MODID);

    public static final Supplier<AttachmentType<DailyRewardData>> DATA = ATTACHMENTS.register("daily_reward",
            () -> AttachmentType.builder(() -> DailyRewardData.EMPTY).serialize(DailyRewardData.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<DailyMissionData>> MISSION_DATA = ATTACHMENTS.register("daily_missions",
            () -> AttachmentType.builder(() -> DailyMissionData.EMPTY).serialize(DailyMissionData.CODEC).copyOnDeath().build());

    public enum Status { CLAIMED, ALREADY_CLAIMED, DISABLED, NO_REWARDS }

    /** An extra reward on top of the day: {@code type} is {@link RewardTableManager#WEEK_BONUS} or {@link RewardTableManager#COMEBACK_BONUS}. */
    public record BonusResult(String type, int days, List<ItemStack> items, int experience) {}

    public record ClaimResult(Status status, int streak, Component tier, List<ItemStack> items, int experience, List<BonusResult> bonuses) {
        static ClaimResult of(Status status) {
            return new ClaimResult(status, 0, Component.empty(), List.of(), 0, List.of());
        }
    }

    public static long today(Instant now) {
        return DailyClock.day(now, DailyConfig.zone(), DailyConfig.resetHour());
    }

    public static DailyRewardData data(ServerPlayer player) {
        return player.getData(DATA);
    }

    public static boolean canClaim(ServerPlayer player, Instant now) {
        return DailyConfig.DAILY_REWARDS_ENABLED.get() && data(player).canClaim(today(now));
    }

    public static ClaimResult claim(ServerPlayer player, Instant now) {
        if (!DailyConfig.DAILY_REWARDS_ENABLED.get()) {
            return ClaimResult.of(Status.DISABLED);
        }
        long today = today(now);
        DailyRewardData data = data(player);
        if (!data.canClaim(today)) {
            return ClaimResult.of(Status.ALREADY_CLAIMED);
        }
        DailyRewardData updated = data.claimed(today, DailyConfig.DAILY_STREAK_GRACE_DAYS.get());
        RewardTable table = RewardTableManager.INSTANCE.forStreak(updated.streak());
        if (table == null) {
            return ClaimResult.of(Status.NO_REWARDS);
        }

        // Save first, then pay out: a crash in between can never hand out the reward twice.
        player.setData(DATA, updated);
        List<ItemStack> items = table.roll(player.getRandom());
        Rewards.give(player, items, table.experience());

        List<BonusResult> bonuses = new ArrayList<>();
        if (isWeekBonus(updated.streak(), DailyConfig.DAILY_WEEK_BONUS_EVERY.get())) {
            giveBonus(player, RewardTableManager.WEEK_BONUS, updated.streak(), bonuses);
        }
        if (isComeback(data, updated, DailyConfig.DAILY_COMEBACK_MIN_STREAK.get())) {
            giveBonus(player, RewardTableManager.COMEBACK_BONUS, data.streak(), bonuses);
        }
        return new ClaimResult(Status.CLAIMED, updated.streak(), table.tierName(), items, table.experience(), List.copyOf(bonuses));
    }

    /** Every {@code every}th day in a row (7, 14, 21, ...) earns the week bonus; 0 turns it off. */
    public static boolean isWeekBonus(int streak, int every) {
        return every > 0 && streak > 0 && streak % every == 0;
    }

    /** A streak of at least {@code minStreak} days was just lost; the comeback reward softens the reset to day 1. */
    public static boolean isComeback(DailyRewardData before, DailyRewardData after, int minStreak) {
        return minStreak > 0 && before.lastClaimDay() != DailyRewardData.NEVER && after.streak() == 1 && before.streak() >= minStreak;
    }

    private static void giveBonus(ServerPlayer player, String type, int days, List<BonusResult> out) {
        RewardTable bonus = RewardTableManager.INSTANCE.bonus(type);
        if (bonus != null) {
            List<ItemStack> items = bonus.roll(player.getRandom());
            Rewards.give(player, items, bonus.experience());
            out.add(new BonusResult(type, days, items, bonus.experience()));
        }
    }

    public static void sendResult(ServerPlayer player, ClaimResult result, Instant now) {
        switch (result.status()) {
            case CLAIMED -> {
                player.sendSystemMessage(Component.translatable("message.gsmod.daily.claimed", result.streak()).withStyle(ChatFormatting.GREEN));
                if (!result.tier().getString().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.gsmod.daily.tier", result.tier()).withStyle(ChatFormatting.GOLD));
                }
                listItems(player, result.items(), result.experience());
                for (BonusResult bonus : result.bonuses()) {
                    player.sendSystemMessage(Component.translatable("message.gsmod.daily." + bonus.type() + "_bonus", bonus.days())
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    listItems(player, bonus.items(), bonus.experience());
                }
            }
            case ALREADY_CLAIMED -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.already",
                    DailyClock.formatDuration(DailyClock.secondsUntilReset(now))).withStyle(ChatFormatting.YELLOW));
            case DISABLED -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.disabled").withStyle(ChatFormatting.RED));
            case NO_REWARDS -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.empty").withStyle(ChatFormatting.RED));
        }
    }

    private static void listItems(ServerPlayer player, List<ItemStack> items, int experience) {
        for (ItemStack stack : items) {
            player.sendSystemMessage(Component.literal(" + " + stack.getCount() + "x ").append(stack.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (experience > 0) {
            player.sendSystemMessage(Component.translatable("message.gsmod.daily.experience", experience).withStyle(ChatFormatting.GRAY));
        }
    }

    private DailyRewards() {}
}
