package vinn.tekk.screwyourmobs.client;

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
        PacketDistributor.sendToServer(RequestSyncPacket.INSTANCE);
        UpdateChecker.checkOnce();

        ScrewYourMobsMod.LOGGER.debug(
                "[ScrewYourMobs!] Client joined - requested rule sync.");
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RuleManager.clearClientCache();

        ScrewYourMobsMod.LOGGER.debug(
                "[ScrewYourMobs!] Client disconnected - cleared rule cache.");
    }
}