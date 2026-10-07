package vinn.tekk.screwyourmobs.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.debug.DebugLog;
import vinn.tekk.screwyourmobs.rules.RuleManager;

import java.util.ArrayList;
import java.util.List;

public final class EntityPurger {

    private EntityPurger() {}

    public static int purgeAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        if (!RuleManager.hasAnyRules()) return 0;

        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            total += purgeLevel(level);
        }
        if (total > 0) {
            DebugLog.log(DebugLog.Channel.REMOVAL,
                    "Purged %d existing entities across all dimensions.", total);
        }
        return total;
    }

    private static int purgeLevel(ServerLevel level) {
        ResourceLocation dimId = level.dimension().location();
        List<Entity> toRemove = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (shouldPurge(entity, dimId)) toRemove.add(entity);
        }

        for (Entity entity : toRemove) {
            if (EntityRemovalConfig.DEBUG_ENABLED.get()) {
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                DebugLog.log(DebugLog.Channel.REMOVAL,
                        "Purged existing: %s in %s", typeId, dimId);
            }
            entity.discard();
        }
        return toRemove.size();
    }

    private static boolean shouldPurge(Entity entity, ResourceLocation dimId) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null) return false;

        if (EntityRemovalConfig.getEntitiesToKeep().contains(typeId.toString())) return false;
        if (entity instanceof Mob mob && mob.hasCustomName()) return false;
        if (entity instanceof TamableAnimal tame && tame.isTame()) return false;

        return RuleManager.findMatch(typeId, dimId, entity.getType()) != null;
    }
}