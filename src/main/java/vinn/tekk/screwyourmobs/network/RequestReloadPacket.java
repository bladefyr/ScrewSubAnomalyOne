package vinn.tekk.screwyourmobs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.procedures.EntityPurger;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import vinn.tekk.screwyourmobs.rules.RuleSetSnapshot;

public record RequestReloadPacket() implements CustomPacketPayload {

    public static final Type<RequestReloadPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ScrewYourMobsMod.MODID, "request_reload"));

    public static final RequestReloadPacket INSTANCE = new RequestReloadPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestReloadPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestReloadPacket> type() {
        return TYPE;
    }

    public static void handle(RequestReloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!player.hasPermissions(2)) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Player {} tried to reload without permission.",
                        player.getName().getString());
                return;
            }

            RuleManager.reload();
            EntityPurger.purgeAll();

            PacketDistributor.sendToAllPlayers(
                    SyncRulesPacket.of(RuleSetSnapshot.capture()));
        });
    }
}