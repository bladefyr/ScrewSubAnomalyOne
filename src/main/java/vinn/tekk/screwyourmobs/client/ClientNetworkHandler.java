package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.network.RequestSyncPacket;
import vinn.tekk.screwyourmobs.rules.RuleManager;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public class ClientNetworkHandler {

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Ask the server to send us the rule set on join
        PacketDistributor.sendToServer(RequestSyncPacket.INSTANCE);

        ScrewYourMobsMod.LOGGER.info(
                "[ScrewYourMobs!] Client joined — requested rule sync.");
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Clear the client cache so we don't show stale rules next connection
        RuleManager.clearClientCache();

        ScrewYourMobsMod.LOGGER.info(
                "[ScrewYourMobs!] Client disconnected — cleared rule cache.");
    }
}