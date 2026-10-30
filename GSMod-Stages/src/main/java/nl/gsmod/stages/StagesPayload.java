package nl.gsmod.stages;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server → client: the player's end-game stage, for the button on the Daily screen and for hiding locked
 * items in JEI. Display only; the server checks everything itself.
 */
public record StagesPayload(boolean worldOpen, boolean unlocked, boolean access, boolean requireUnlock) implements CustomPacketPayload {
    public static final Type<StagesPayload> TYPE = new Type<>(GSModStages.id("stage_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StagesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StagesPayload::worldOpen,
            ByteBufCodecs.BOOL, StagesPayload::unlocked,
            ByteBufCodecs.BOOL, StagesPayload::access,
            ByteBufCodecs.BOOL, StagesPayload::requireUnlock,
            StagesPayload::new);

    public static StagesPayload of(ServerPlayer player) {
        return new StagesPayload(Stages.worldOpen(player.level().getServer()), Stages.playerUnlocked(player), Stages.hasAccess(player),
                StagesConfig.REQUIRE_PLAYER_UNLOCK.get());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
