package vinn.tekk.screwyourmobs.procedures;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleManager;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerLifecycleHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RuleManager.reload();
        EntityPurger.purgeAll();
    }
}