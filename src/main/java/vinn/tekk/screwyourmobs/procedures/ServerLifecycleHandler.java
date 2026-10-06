package vinn.tekk.screwyourmobs.procedures;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = ScrewYourMobsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerLifecycleHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RuleManager.reload();
        EntityPurger.purgeAll();
    }
}