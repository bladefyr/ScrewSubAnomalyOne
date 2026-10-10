package vinn.tekk.screwyourmobs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
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
                player.sendSystemMessage(Component.literal(
                        "§c[ScrewYourMobs!] You don't have permission to reload rules."));
                return;
            }

            RuleManager.reload();
            EntityPurger.purgeAll();

            int rules = RuleManager.getTotalRules();
            int warnings = RuleManager.getLastWarnings().size();

            player.sendSystemMessage(Component.literal(
                    "§a[ScrewYourMobs!] §fReloaded §a" + rules + " §frules, §a"
                            + warnings + " §fvalidation warning(s)."));

            if (EntityRemovalConfig.DEBUG_ENABLED.get()
                    && EntityRemovalConfig.DEBUG_VALIDATE.get()
                    && EntityRemovalConfig.DEBUG_TO_CHAT.get()) {
                for (String w : RuleManager.getLastWarnings()) {
                    player.sendSystemMessage(Component.literal(
                            "§7[§bDEBUG/validate§7] §f" + w));
                }
            }

            PacketDistributor.sendToAllPlayers(
                    SyncRulesPacket.of(RuleSetSnapshot.capture()));
        });
    }
}