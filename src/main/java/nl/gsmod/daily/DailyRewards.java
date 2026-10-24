package nl.gsmod.daily;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.gsmod.ServerConfig;
import nl.gsmod.GSMod;

/**
 * Daily login reward. Everything runs on the server: the client only sends the {@code /daily}
 * command, and the server decides from its own clock and the stored player data whether a reward is due.
 */
public final class DailyRewards {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GSMod.MODID);

    public static final Supplier<AttachmentType<DailyRewardData>> DATA = ATTACHMENTS.register("daily_reward",
            () -> AttachmentType.builder(() -> DailyRewardData.EMPTY).serialize(DailyRewardData.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<DailyMissionData>> MISSION_DATA = ATTACHMENTS.register("daily_missions",
            () -> AttachmentType.builder(() -> DailyMissionData.EMPTY).serialize(DailyMissionData.CODEC).copyOnDeath().build());

    public enum Status { CLAIMED, ALREADY_CLAIMED, DISABLED, NO_REWARDS }

    public record ClaimResult(Status status, int streak, Component tier, List<ItemStack> items, int experience) {
        static ClaimResult of(Status status) {
            return new ClaimResult(status, 0, Component.empty(), List.of(), 0);
        }
    }

    public static long today(Instant now) {
        return DailyClock.day(now, ServerConfig.DAILY_RESET_HOUR_UTC.get());
    }

    public static DailyRewardData data(ServerPlayer player) {
        return player.getData(DATA);
    }

    public static boolean canClaim(ServerPlayer player, Instant now) {
        return ServerConfig.DAILY_REWARDS_ENABLED.get() && data(player).canClaim(today(now));
    }

    public static ClaimResult claim(ServerPlayer player, Instant now) {
        if (!ServerConfig.DAILY_REWARDS_ENABLED.get()) {
            return ClaimResult.of(Status.DISABLED);
        }
        long today = today(now);
        DailyRewardData data = data(player);
        if (!data.canClaim(today)) {
            return ClaimResult.of(Status.ALREADY_CLAIMED);
        }
        DailyRewardData updated = data.claimed(today, ServerConfig.DAILY_STREAK_GRACE_DAYS.get());
        RewardTable table = RewardTableManager.INSTANCE.forStreak(updated.streak());
        if (table == null) {
            return ClaimResult.of(Status.NO_REWARDS);
        }

        // Save first, then pay out: a crash in between can never hand out the reward twice.
        player.setData(DATA, updated);
        List<ItemStack> items = table.roll(player.getRandom());
        Rewards.give(player, items, table.experience());
        return new ClaimResult(Status.CLAIMED, updated.streak(), table.tierName(), items, table.experience());
    }

    public static void sendResult(ServerPlayer player, ClaimResult result, Instant now) {
        switch (result.status()) {
            case CLAIMED -> {
                player.sendSystemMessage(Component.translatable("message.gsmod.daily.claimed", result.streak()).withStyle(ChatFormatting.GREEN));
                if (!result.tier().getString().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.gsmod.daily.tier", result.tier()).withStyle(ChatFormatting.GOLD));
                }
                for (ItemStack stack : result.items()) {
                    player.sendSystemMessage(Component.literal(" + " + stack.getCount() + "x ").append(stack.getHoverName()).withStyle(ChatFormatting.GRAY));
                }
                if (result.experience() > 0) {
                    player.sendSystemMessage(Component.translatable("message.gsmod.daily.experience", result.experience()).withStyle(ChatFormatting.GRAY));
                }
            }
            case ALREADY_CLAIMED -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.already",
                    DailyClock.formatDuration(DailyClock.secondsUntilReset(now, ServerConfig.DAILY_RESET_HOUR_UTC.get()))).withStyle(ChatFormatting.YELLOW));
            case DISABLED -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.disabled").withStyle(ChatFormatting.RED));
            case NO_REWARDS -> player.sendSystemMessage(Component.translatable("message.gsmod.daily.empty").withStyle(ChatFormatting.RED));
        }
    }

    private DailyRewards() {}
}
