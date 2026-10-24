package nl.gsmod.daily;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player daily reward state, stored on the player as a NeoForge data attachment
 * (saved with the player file, kept on death and dimension change).
 *
 * @param lastClaimDay reward day number of the last claim (see {@link DailyClock}), or {@link #NEVER}
 * @param streak       consecutive days claimed, including the last claim
 * @param totalClaims  lifetime number of claims
 */
public record DailyRewardData(long lastClaimDay, int streak, int totalClaims) {
    public static final long NEVER = Long.MIN_VALUE;
    public static final DailyRewardData EMPTY = new DailyRewardData(NEVER, 0, 0);

    public static final MapCodec<DailyRewardData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("last_claim_day", NEVER).forGetter(DailyRewardData::lastClaimDay),
            Codec.INT.optionalFieldOf("streak", 0).forGetter(DailyRewardData::streak),
            Codec.INT.optionalFieldOf("total_claims", 0).forGetter(DailyRewardData::totalClaims)
    ).apply(i, DailyRewardData::new));

    public boolean canClaim(long today) {
        // A claim day in the future means the server clock went backwards: refuse until it catches up.
        return lastClaimDay == NEVER || today > lastClaimDay;
    }

    /** Streak the player will have after claiming on {@code today}. */
    public int nextStreak(long today, int graceDays) {
        if (lastClaimDay == NEVER || streak <= 0) {
            return 1;
        }
        long missed = today - lastClaimDay - 1;
        return missed <= graceDays ? streak + 1 : 1;
    }

    public DailyRewardData claimed(long today, int graceDays) {
        return new DailyRewardData(today, nextStreak(today, graceDays), totalClaims + 1);
    }
}
