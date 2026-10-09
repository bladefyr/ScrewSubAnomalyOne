package vinn.tekk.screwyourmobs.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import vinn.tekk.screwyourmobs.rules.RuleSetSnapshot;

/**
 * Server → Client. Pushes the current rule set so the client's
 * RuleManager cache reflects the server state.
 */
public record SyncRulesPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncRulesPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ScrewYourMobsMod.MODID, "sync_rules"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRulesPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeNbt(packet.tag()),
                    buf -> new SyncRulesPacket(buf.readNbt())
            );

    public static SyncRulesPacket of(RuleSetSnapshot snapshot) {
        return new SyncRulesPacket(snapshot.toNbt());
    }

    @Override
    public Type<SyncRulesPacket> type() {
        return TYPE;
    }

    // ---- Handler ----

    public static void handle(SyncRulesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            RuleSetSnapshot snapshot = RuleSetSnapshot.fromNbt(packet.tag());
            RuleManager.applySync(snapshot);

            ScrewYourMobsMod.LOGGER.info(
                    "[ScrewYourMobs!] Synced {} rules from server.",
                    snapshot.rules().size());
        });
    }
}