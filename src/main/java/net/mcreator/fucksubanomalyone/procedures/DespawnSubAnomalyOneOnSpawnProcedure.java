package net.mcreator.fucksubanomalyone.procedures;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.fucksubanomalyone.FuckSubanomalyoneMod;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DespawnSubAnomalyOneOnSpawnProcedure {
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinLevelEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if ((BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()).equals("thebrokenscript:sub_anomaly_1")) {
			FuckSubanomalyoneMod.LOGGER.info("\u00A76Despawned SubAnomaly1");
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
