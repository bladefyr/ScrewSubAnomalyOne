package com.bladelicious.screwsubanomalyone.procedures;

import com.bladelicious.screwsubanomalyone.ScrewSubanomalyoneMod;
import com.bladelicious.screwsubanomalyone.config.EntityRemovalConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber
public class DespawnHim {
    private static final Set<ResourceLocation> ENTITIES_TO_REMOVE = new HashSet<>();
    private static boolean configLoaded = false;

    private static void loadConfig() {
        ENTITIES_TO_REMOVE.clear();

        try {
            // Use the helper method to parse the comma-separated string
            for (String entityId : EntityRemovalConfig.getEntityList()) {
                try {
                    ResourceLocation resourceLocation = ResourceLocation.parse(entityId);
                    ENTITIES_TO_REMOVE.add(resourceLocation);
                    ScrewSubanomalyoneMod.LOGGER.debug("Added entity to removal list: {}", resourceLocation);
                } catch (Exception e) {
                    ScrewSubanomalyoneMod.LOGGER.error("Invalid entity ID in config: {}", entityId);
                }
            }
        } catch (Exception e) {
            ScrewSubanomalyoneMod.LOGGER.error("Error loading config", e);
        }

        configLoaded = true;
        ScrewSubanomalyoneMod.LOGGER.info("Loaded {} entities for removal", ENTITIES_TO_REMOVE.size());
    }

    public static void markConfigForReload() {
        configLoaded = false;
        ScrewSubanomalyoneMod.LOGGER.info("Config marked for reload");
    }

    @SubscribeEvent
    public static void onEntitySpawned(EntityJoinLevelEvent event) {
        if (!configLoaded) {
            loadConfig();
        }

        if (ENTITIES_TO_REMOVE.isEmpty()) {
            return;
        }

        Entity entity = event.getEntity();
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (entityType != null && ENTITIES_TO_REMOVE.contains(entityType)) {
            if (EntityRemovalConfig.SPEC.enableLogging.get()) {
                String message = String.format(EntityRemovalConfig.SPEC.logMessage.get(), entityType.toString());
                ScrewSubanomalyoneMod.LOGGER.info(message);
            }
            entity.discard();
            event.setCanceled(true);
        }
    }
}