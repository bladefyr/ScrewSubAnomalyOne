package vinn.tekk.screwyourmobs.procedures;

import vinn.tekk.screwyourmobs.ScrewYourMobsMod;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.rules.RemovalRule;
import vinn.tekk.screwyourmobs.rules.RuleManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes already-loaded entities that match the current rule set.
 * Used on /sym reload and on server start, so existing mobs don't linger
 * until they unload or die naturally.
 */
public final class EntityPurger {

    private EntityPurger() {}

    /**
     * Runs a purge pass across all loaded dimensions.
     * Safe to call from the server thread only.
     *
     * @return the number of entities removed.
     */
    public static int purgeAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ScrewYourMobsMod.LOGGER.warn("[ScrewYourMobs!] Purge skipped, no server.");
            return 0;
        }

        if (!RuleManager.hasAnyRules()) {
            return 0;
        }

        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            total += purgeLevel(level);
        }

        if (total > 0) {
            ScrewYourMobsMod.LOGGER.info(
                    "[ScrewYourMobs!] Purged {} existing entities across all dimensions.",
                    total);
        }
        return total;
    }

    private static int purgeLevel(ServerLevel level) {
        ResourceLocation dimId = level.dimension().location();
        List<Entity> toRemove = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (!shouldPurge(entity, dimId)) continue;
            toRemove.add(entity);
        }

        for (Entity entity : toRemove) {
            if (EntityRemovalConfig.ENABLE_LOGGING.get()) {
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                ScrewYourMobsMod.LOGGER.info(
                        "[ScrewYourMobs!] Purged existing: {} in {}",
                        typeId, dimId);
            }
            entity.discard();
        }

        return toRemove.size();
    }

    private static boolean shouldPurge(Entity entity, ResourceLocation dimId) {
        EntityType<?> type = entity.getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null) return false;

        // Keep-list override
        if (EntityRemovalConfig.getEntitiesToKeep().contains(typeId.toString())) {
            return false;
        }

        // Optional: don't purge named mobs (comment this block out if you want them gone too)
        if (entity instanceof Mob mob && mob.hasCustomName()) {
            return false;
        }

        // Optional: don't purge tamed mobs
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame()) {
            return false;
        }

        return RuleManager.findMatch(typeId, dimId) != null;
    }
}