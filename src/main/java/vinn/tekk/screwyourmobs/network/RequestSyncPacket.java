package vinn.tekk.screwyourmobs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleSetSnapshot;

/**
 * Client → Server. Asks the server to send a fresh SyncRulesPacket.
 * Fired on player join and after any client-side action that expects a refresh.
 */
public record RequestSyncPacket() implements CustomPacketPayload {

    public static final Type<RequestSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ScrewYourMobsMod.MODID, "request_sync"));

    public static final RequestSyncPacket INSTANCE = new RequestSyncPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSyncPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestSyncPacket> type() {
        return TYPE;
    }

    // ---- Handler ----

    public static void handle(RequestSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Reply only to the requester with a fresh snapshot
            PacketDistributor.sendToPlayer(player,
                    SyncRulesPacket.of(RuleSetSnapshot.capture()));
        });
    }
}