package vinn.tekk.screwyourmobs.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                SyncRulesPacket.TYPE,
                SyncRulesPacket.STREAM_CODEC,
                SyncRulesPacket::handle
        );

        registrar.playToServer(
                RequestSyncPacket.TYPE,
                RequestSyncPacket.STREAM_CODEC,
                RequestSyncPacket::handle
        );

        registrar.playToServer(
                MutateRulePacket.TYPE,
                MutateRulePacket.STREAM_CODEC,
                MutateRulePacket::handle
        );

        registrar.playToServer(
                RequestReloadPacket.TYPE,
                RequestReloadPacket.STREAM_CODEC,
                RequestReloadPacket::handle
        );
    }
}