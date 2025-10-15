package com.bladelicious.screwsubanomalyone.procedures;

import com.bladelicious.screwsubanomalyone.ScrewSubanomalyoneMod;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber
public class DespawnHim {
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinLevelEvent event) {
		assert event != null;

		Entity entity = event.getEntity();
		if (entity.getType().toString().equals("entity.thebrokenscript.sub_anomaly_1")) {
			ScrewSubanomalyoneMod.LOGGER.info("§6Despawned SubAnomaly1");
			entity.discard();
		}
	}
}