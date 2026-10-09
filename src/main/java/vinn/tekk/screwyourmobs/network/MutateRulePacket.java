package vinn.tekk.screwyourmobs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleLoader;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import vinn.tekk.screwyourmobs.rules.RuleSetSnapshot;
import vinn.tekk.screwyourmobs.rules.RuleWriter;

import java.nio.file.Path;

/**
 * Client → Server. Requests a single rule mutation. Server validates,
 * runs it, then broadcasts a fresh SyncRulesPacket to all connected players.
 */
public record MutateRulePacket(Operation op, String ruleName, String payload)
        implements CustomPacketPayload {

    public enum Operation {
        ADD_ENTITY,
        REMOVE_ENTITY,
        ADD_DIMENSION,
        REMOVE_DIMENSION,
        RENAME,
        CREATE,
        DELETE,
        TOGGLE_DISABLED
    }

    public static final Type<MutateRulePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ScrewYourMobsMod.MODID, "mutate_rule"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutateRulePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.map(i -> Operation.values()[i], Enum::ordinal),
                    MutateRulePacket::op,
                    ByteBufCodecs.STRING_UTF8,
                    MutateRulePacket::ruleName,
                    ByteBufCodecs.STRING_UTF8,
                    MutateRulePacket::payload,
                    MutateRulePacket::new
            );

    @Override
    public Type<MutateRulePacket> type() {
        return TYPE;
    }

    // ---- Server handler ----

    public static void handle(MutateRulePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Same permission gate as /sym — OP level 2
            if (!player.hasPermissions(2)) {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Player {} tried to mutate rule '{}' without permission.",
                        player.getName().getString(), packet.ruleName());
                return;
            }

            boolean ok = applyMutation(packet, player);

            if (ok) {
                // Reload the server-side rules from disk
                RuleManager.reload();

                // Broadcast the new state to every connected client
                PacketDistributor.sendToAllPlayers(
                        SyncRulesPacket.of(RuleSetSnapshot.capture()));
            } else {
                ScrewYourMobsMod.LOGGER.warn(
                        "[ScrewYourMobs!] Mutation {} on rule '{}' failed.",
                        packet.op(), packet.ruleName());
            }
        });
    }

    private static boolean applyMutation(MutateRulePacket packet, ServerPlayer player) {
        return switch (packet.op()) {
            case ADD_ENTITY -> RuleWriter.addEntity(packet.ruleName(), packet.payload());
            case REMOVE_ENTITY -> RuleWriter.removeEntity(packet.ruleName(), packet.payload());
            case ADD_DIMENSION -> RuleWriter.addDimension(packet.ruleName(), packet.payload());
            case REMOVE_DIMENSION -> RuleWriter.removeDimension(packet.ruleName(), packet.payload());
            case RENAME -> RuleWriter.renameRule(packet.ruleName(), packet.payload());
            case DELETE -> RuleManager.deleteRule(packet.ruleName());
            case TOGGLE_DISABLED -> RuleManager.toggleDisabled(packet.ruleName());
            case CREATE -> {
                Path worldRulesDir = player.server.getWorldPath(LevelResource.ROOT)
                        .resolve(RuleLoader.WORLD_RULES_FOLDER);
                yield RuleWriter.createRule(packet.payload(), worldRulesDir);
            }
        };
    }
}